export const getScheduleStartAt = (schedule) => {
  const scheduleDate = String(schedule?.scheduleDate || '').slice(0, 10)
  const startTime = String(schedule?.startTime || '').slice(0, 8)

  if (!scheduleDate || !startTime) {
    return null
  }

  const normalizedTime = startTime.length === 5 ? `${startTime}:00` : startTime
  const startAt = new Date(`${scheduleDate}T${normalizedTime}`)
  return Number.isNaN(startAt.getTime()) ? null : startAt
}

export const hasScheduleStarted = (schedule, nowInput = Date.now()) => {
  const startAt = getScheduleStartAt(schedule)
  if (!startAt) {
    return false
  }

  const now = nowInput instanceof Date ? nowInput : new Date(nowInput)
  return now.getTime() >= startAt.getTime()
}
