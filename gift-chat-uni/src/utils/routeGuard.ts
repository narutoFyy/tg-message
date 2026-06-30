import type { SessionUser } from '@/types'

const USER_SUPPORT_ROUTE = '/pages/support/index'
const STAFF_SUPPORT_ROUTE = '/pages/support-chat-v2/index'

export function isStaff(user?: Pick<SessionUser, 'roleCode'> | null) {
  return user?.roleCode === 'AGENT' || user?.roleCode === 'ADMIN'
}

export function safeRouteForRole(route: string | undefined, user?: Pick<SessionUser, 'roleCode'> | null, fallback = USER_SUPPORT_ROUTE) {
  const target = route || fallback
  if (target.startsWith(STAFF_SUPPORT_ROUTE) && !isStaff(user)) {
    return USER_SUPPORT_ROUTE
  }
  if (target.startsWith(USER_SUPPORT_ROUTE) && isStaff(user)) {
    return STAFF_SUPPORT_ROUTE
  }
  return target
}

export function supportRouteForRole(user?: Pick<SessionUser, 'roleCode'> | null) {
  return isStaff(user) ? STAFF_SUPPORT_ROUTE : USER_SUPPORT_ROUTE
}
