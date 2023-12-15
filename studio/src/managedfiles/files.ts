export function prettyPrintInBytes(bytes: number): string {
  const units = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB']
  let unitIndex = 0
  let value = bytes

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex++
  }

  return `${value.toFixed(2)} ${units[unitIndex]}`
}
