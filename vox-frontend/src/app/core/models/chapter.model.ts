import { NovelCharacterResponse } from './character.model';

export type ChapterStatus =
  | 'DRAFT'
  | 'PROCESSING_AI'
  | 'REVIEWING'
  | 'PUBLISHED'
  | 'FAILED';

export interface ScriptLineDto {
  sequence: number;
  characterId: string;
  text: string;
  audioDraftLink?: string | null;
  status?: string | null;
}

export interface ChapterResponse {
  id: number;
  novelId: number;
  chapterNumber: number;
  title: string;
  originalContent: string;
  scriptData?: ScriptLineDto[] | null;
  characters?: NovelCharacterResponse[];
  audioUrl?: string | null;
  hasAudio?: boolean;
  status: ChapterStatus;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateChapterRequest {
  novelId: number;
  title: string;
  chapterNumber: number;
  originalContent: string;
}

export interface UpdateChapterRequest {
  title: string;
  chapterNumber: number;
  originalContent: string;
  status?: ChapterStatus;
  audioUrl?: string | null;
  hasAudio?: boolean;
}

export interface TriggerAiScriptRequest {
  chapterId: number;
  characterIds: number[];
}

export const CHAPTER_STATUS_OPTIONS: { value: ChapterStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Bản nháp' },
  { value: 'PROCESSING_AI', label: 'Đang xử lý AI' },
  { value: 'REVIEWING', label: 'Đang duyệt' },
  { value: 'PUBLISHED', label: 'Đã xuất bản' },
  { value: 'FAILED', label: 'Thất bại' },
];

export function chapterStatusLabel(status: ChapterStatus | string): string {
  return CHAPTER_STATUS_OPTIONS.find((o) => o.value === status)?.label ?? status;
}
