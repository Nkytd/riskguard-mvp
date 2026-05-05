import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  clearSession,
  formatApiError,
  getStoredUser,
  getToken,
  loadCurrentUser,
  loadRules,
  login,
  saveSession,
} from './client'

const user = {
  id: 1,
  username: 'admin',
  realName: 'Administrator',
  roleCode: 'ADMIN',
}

function jsonResponse<T>(data: T, init: { ok?: boolean; status?: number; code?: number; message?: string } = {}) {
  return {
    ok: init.ok ?? true,
    status: init.status ?? 200,
    statusText: init.message ?? 'OK',
    json: vi.fn().mockResolvedValue({
      code: init.code ?? 0,
      message: init.message ?? 'OK',
      data,
      traceId: 'TRACE-1',
      timestamp: '2026-05-05T00:00:00',
    }),
  } as unknown as Response
}

function mockFetch(response: Response) {
  const fetchMock = vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function createLocalStorageMock(): Storage {
  const store = new Map<string, string>()
  return {
    get length() {
      return store.size
    },
    clear: vi.fn(() => store.clear()),
    getItem: vi.fn((key: string) => store.get(key) ?? null),
    key: vi.fn((index: number) => Array.from(store.keys())[index] ?? null),
    removeItem: vi.fn((key: string) => {
      store.delete(key)
    }),
    setItem: vi.fn((key: string, value: string) => {
      store.set(key, value)
    }),
  }
}

beforeEach(() => {
  vi.stubGlobal('localStorage', createLocalStorageMock())
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('session storage', () => {
  it('saves, reads, and clears the admin session', () => {
    saveSession('ACCESS_TOKEN', user)

    expect(getToken()).toBe('ACCESS_TOKEN')
    expect(getStoredUser()).toEqual(user)

    clearSession()

    expect(getToken()).toBeNull()
    expect(getStoredUser()).toBeNull()
  })

  it('returns null for a corrupted stored user', () => {
    localStorage.setItem('riskguard.user', '{broken json')

    expect(getStoredUser()).toBeNull()
  })
})

describe('formatApiError', () => {
  it('maps authorization errors to operator-facing messages', () => {
    expect(formatApiError(new ApiError('Unauthorized', 401), 'Fallback')).toBe('Session expired. Sign in again.')
    expect(formatApiError(new ApiError('Forbidden', 403), 'Fallback')).toBe('You do not have permission to perform this action.')
  })

  it('maps network failures and unknown values to stable fallback copy', () => {
    expect(formatApiError(new TypeError('Failed to fetch'), 'Fallback')).toBe(
      'Network error. Check that the backend service is running and try again.',
    )
    expect(formatApiError(null, 'Fallback')).toBe('Fallback')
  })
})

describe('requests', () => {
  it('adds bearer tokens and parses current user payloads', async () => {
    saveSession('ACCESS_TOKEN', user)
    const fetchMock = mockFetch(jsonResponse(user))

    await expect(loadCurrentUser()).resolves.toEqual(user)

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/auth/me', {
      headers: expect.any(Headers),
    })
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect((init.headers as Headers).get('Authorization')).toBe('Bearer ACCESS_TOKEN')
  })

  it('trims query filters and omits empty filters', async () => {
    const fetchMock = mockFetch(jsonResponse({
      records: [],
      total: 0,
      pageNo: 2,
      pageSize: 10,
    }))

    await loadRules({
      pageNo: 2,
      pageSize: 10,
      eventType: '',
      status: '',
      keyword: '  payment  ',
    })

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/rules?pageNo=2&pageSize=10&keyword=payment', {
      headers: expect.any(Headers),
    })
  })

  it('clears the local session on unauthorized API responses', async () => {
    saveSession('ACCESS_TOKEN', user)
    mockFetch(jsonResponse(null, {
      ok: false,
      status: 401,
      code: 401,
      message: 'Unauthorized',
    }))

    await expect(loadCurrentUser()).rejects.toMatchObject({ code: 401 })

    expect(getToken()).toBeNull()
    expect(getStoredUser()).toBeNull()
  })

  it('normalizes fetch failures into ApiError', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(login('admin', 'secret')).rejects.toMatchObject({
      code: 0,
      message: 'Network error. Check that the backend service is running and try again.',
    })
  })
})
