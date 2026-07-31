import type { Clock } from "../application/ports/Clock";

export class FixedClock implements Clock {
  constructor(private currentTime: Date) {}

  now(): Date {
    return this.currentTime;
  }

  advanceBy(ms: number): void {
    this.currentTime = new Date(this.currentTime.getTime() + ms);
  }
}
