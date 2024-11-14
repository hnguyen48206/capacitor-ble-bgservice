export interface BLEServPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    startService(): Promise<void>;
    stopService(): Promise<void>;
}
