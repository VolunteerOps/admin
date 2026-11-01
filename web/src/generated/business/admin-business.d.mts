type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function validEmail(value: string): boolean;
export declare function canCoordinate(role: string, active: boolean, approved: boolean, organization: string): boolean;
export declare function recordStatuses(kind: string): Array<string>;
export declare function missionTransitions(status: string): Array<string>;
export declare function availableStock(quantity: number, reserved: number): number;
export declare function coverage(assigned: number, capacity: number): number;
export declare function missionError(title: string, brief: string, capacity: number, start: number, end: number, latitude: number, longitude: number): string;