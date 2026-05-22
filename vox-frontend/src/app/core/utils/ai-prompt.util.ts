import { VoiceResponse } from '../models/voice.model';

export interface CharacterAiPromptContext {
  novelId: number;
  voices: VoiceResponse[];
  originalContent?: string;
}

export function buildCharacterListAiPrompt(ctx: CharacterAiPromptContext): string {
  const voicesBlock =
    ctx.voices.length === 0
      ? '(Chưa có danh sách giọng — hãy gọi GET /api/voices)'
      : ctx.voices
          .map(
            (v) =>
              `- id=${v.id}, name="${v.name}", gender=${v.gender ?? 'N/A'}, provider=${v.providerCode ?? 'N/A'}, language=${v.language ?? ''}`,
          )
          .join('\n');

  const contentBlock =
    ctx.originalContent?.trim() ||
    '(Chưa có nội dung — hãy dán originalContent của chương vào đây)';

  return `Hãy giúp tôi tạo list data cho API. Mỗi nhân vật là một object trong mảng JSON.

Định dạng mỗi phần tử:
{
  "novelId": ${ctx.novelId},
  "name": "...",
  "gender": "MALE hoặc FEMALE hoặc UNKNOWN",
  "description": "mô tả tone giọng / tính cách cho TTS",
  "defaultVoiceId": 1
}

Danh sách giọng (chọn defaultVoiceId phù hợp):
${voicesBlock}

Quy tắc:
- Dẫn truyện (người kể) dùng giọng nữ banmai, id=2, novelId=0, name=Female_Narrator.
- Các nhân vật khác chọn giọng nam/nữ phù hợp từ danh sách trên.

Dựa vào kịch bản sau, liệt kê TẤT CẢ nhân vật (kể cả dẫn truyện nếu cần), trả về MẢNG JSON thuần (không markdown):

${contentBlock}`;
}

export function openChatGptWithPrompt(prompt: string): void {
  window.open(`https://chatgpt.com/?q=${encodeURIComponent(prompt)}`, '_blank', 'noopener');
}

export async function openGeminiWithPrompt(prompt: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(prompt);
    window.alert('Đã sao chép prompt vào clipboard. Dán vào Gemini sau khi trang mở.');
  } catch {
    window.prompt('Sao chép prompt sau đây và dán vào Gemini:', prompt);
  }
  window.open('https://gemini.google.com/app', '_blank', 'noopener');
}

export async function copyAiPrompt(prompt: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(prompt);
    window.alert('Đã sao chép prompt.');
  } catch {
    window.prompt('Sao chép prompt:', prompt);
  }
}
