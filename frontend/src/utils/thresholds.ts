/** Converte uma string "80, 100" em [80, 100] — usado por limiares de alerta de orçamentos e metas. */
export function toThresholdList(value: string): number[] {
  return value
    .split(',')
    .map((part) => Number(part.trim()))
    .filter((n) => Number.isFinite(n) && n > 0)
}
