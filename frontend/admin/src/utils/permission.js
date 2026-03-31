const parseAdminUser = () => {
  try {
    return JSON.parse(localStorage.getItem('admin-user') || '{}')
  } catch (error) {
    return {}
  }
}

export const getPermissionCodes = () => {
  const adminUser = parseAdminUser()
  return new Set(
    String(adminUser.permissionCodes || '')
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
  )
}

export const getCurrentRole = () => {
  const loginType = localStorage.getItem('admin-login-type') || 'admin'
  if (loginType === 'store') return 'store'
  if (loginType === 'staff') return 'staff'
  return 'admin'
}

export const getStaffRole = () => parseAdminUser().staffRole || ''

export const hasPermissionCode = (permissionCode) => {
  if (!permissionCode) return true
  return getPermissionCodes().has(permissionCode)
}

export const hasAnyPermissionCode = (permissionCodes = []) => {
  if (!permissionCodes || permissionCodes.length === 0) return true
  const currentCodes = getPermissionCodes()
  return permissionCodes.some(code => currentCodes.has(code))
}

export const hasRoutePermission = (route) => {
  const currentRole = getCurrentRole()
  const meta = route?.meta || {}

  if (meta.roles?.length && !meta.roles.includes(currentRole)) {
    return false
  }

  if (currentRole === 'staff') {
    if (meta.staffRoles?.length && !meta.staffRoles.includes(getStaffRole())) {
      return false
    }
    if (meta.permissionCodes?.length && !hasAnyPermissionCode(meta.permissionCodes)) {
      return false
    }
  }

  return true
}

export const canManageEmployees = () => getCurrentRole() !== 'staff' || hasPermissionCode('employee:manage')
export const canViewReports = () => getCurrentRole() !== 'staff' || hasPermissionCode('report:view')
export const canViewNotifications = () => getCurrentRole() !== 'staff' || hasPermissionCode('notification:view')
