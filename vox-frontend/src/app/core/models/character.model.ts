export interface NovelCharacterResponse {
  id: number;
  novelId: number;
  name: string;
  gender?: string;
  description?: string;
  defaultVoiceId?: number | null;
  defaultVoiceName?: string | null;
}

export interface CreateCharacterRequest {
  novelId: number;
  name: string;
  gender?: string;
  description?: string;
  defaultVoiceId?: number | null;
}

export const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
  { value: 'UNKNOWN', label: 'Không xác định' },
];
