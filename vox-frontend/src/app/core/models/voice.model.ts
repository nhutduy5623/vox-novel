export interface VoiceResponse {
  id: number;
  providerId: number;
  providerCode?: string;
  providerVoiceId?: string;
  name: string;
  gender?: string;
  language?: string;
  previewUrl?: string;
}
