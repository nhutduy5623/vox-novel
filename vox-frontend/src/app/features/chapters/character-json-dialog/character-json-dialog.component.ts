import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { ApiError } from '../../../core/models/api-response.model';
import { CreateCharacterRequest } from '../../../core/models/character.model';
import { CharacterService } from '../../../core/services/character.service';

@Component({
  selector: 'app-character-json-dialog',
  imports: [FormsModule],
  templateUrl: './character-json-dialog.component.html',
  styleUrl: './character-json-dialog.component.css',
})
export class CharacterJsonDialogComponent {
  private readonly characterService = inject(CharacterService);

  @Input() open = false;
  @Input() novelId!: number;

  /** Khi set: sau import sẽ emit danh sách id đã tạo để gán vào chương */
  @Input() assignToChapterId: number | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() imported = new EventEmitter<number[]>();

  jsonText = '';
  saving = false;
  errorMessage: string | null = null;
  private createdIds: number[] = [];

  readonly exampleJson = `{
  "name": "Tên nhân vật",
  "gender": "MALE",
  "description": "Mô tả tone giọng",
  "defaultVoiceId": 1
}`;

  readonly exampleArrayJson = `[
  {
    "name": "Nhân vật A",
    "gender": "FEMALE",
    "defaultVoiceId": 2
  }
]`;

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-backdrop')) {
      this.close();
    }
  }

  close(): void {
    if (!this.saving) {
      this.errorMessage = null;
      this.closed.emit();
    }
  }

  useExample(): void {
    this.jsonText = this.exampleJson;
    this.errorMessage = null;
  }

  submit(): void {
    if (this.saving) {
      return;
    }

    let payloads: CreateCharacterRequest[];
    try {
      payloads = this.parseJson(this.jsonText);
    } catch (e) {
      this.errorMessage = e instanceof Error ? e.message : 'JSON không hợp lệ.';
      return;
    }

    if (payloads.length === 0) {
      this.errorMessage = 'Không có nhân vật nào trong JSON.';
      return;
    }

    this.saving = true;
    this.errorMessage = null;
    this.createdIds = [];
    forkJoin({
      existing: this.characterService.getByNovel(this.novelId),
      narrators: this.novelId === 0 ? of([]) : this.characterService.getByNovel(0),
    }).subscribe({
      next: ({ existing, narrators }) => {
        const merged = [...existing, ...narrators];
        const keyToId = new Map<string, number>();
        for (const c of merged) {
          keyToId.set(this.buildKey(c.name, c.defaultVoiceId), c.id);
        }
        this.importCharacters(payloads, 0, keyToId);
      },
      error: () => {
        this.importCharacters(payloads, 0, new Map<string, number>());
      },
    });
  }

  private importCharacters(payloads: CreateCharacterRequest[], index: number, keyToId: Map<string, number>): void {
    if (index >= payloads.length) {
      this.saving = false;
      this.jsonText = '';
      this.imported.emit([...this.createdIds]);
      return;
    }

    const payload = payloads[index];
    const key = this.buildKey(payload.name, payload.defaultVoiceId);
    const existingId = keyToId.get(key);
    if (existingId != null) {
      this.createdIds.push(existingId);
      this.importCharacters(payloads, index + 1, keyToId);
      return;
    }

    this.characterService.create(payload).subscribe({
      next: (saved) => {
        this.createdIds.push(saved.id);
        keyToId.set(key, saved.id);
        this.importCharacters(payloads, index + 1, keyToId);
      },
      error: (err) => {
        this.saving = false;
        const msg =
          err instanceof ApiError
            ? err.message
            : 'Import thất bại.';
        this.errorMessage = `Lỗi tại nhân vật #${index + 1} (${payload.name}): ${msg}`;
      },
    });
  }

  private buildKey(name: string, voiceId: number | null | undefined): string {
    const normalizedName = name.trim().toLowerCase();
    const normalizedVoice = voiceId == null ? 'null' : String(voiceId);
    return `${normalizedName}|${normalizedVoice}`;
  }

  private parseJson(text: string): CreateCharacterRequest[] {
    const trimmed = text.trim();
    if (!trimmed) {
      throw new Error('Nhập JSON nhân vật.');
    }

    const parsed: unknown = JSON.parse(trimmed);
    const items = Array.isArray(parsed) ? parsed : [parsed];

    return items.map((item, i) => {
      if (typeof item !== 'object' || item == null) {
        throw new Error(`Phần tử #${i + 1} phải là object.`);
      }
      const obj = item as Record<string, unknown>;
      const name = String(obj['name'] ?? '').trim();
      if (!name) {
        throw new Error(`Phần tử #${i + 1}: thiếu "name".`);
      }

      const request: CreateCharacterRequest = {
        novelId: this.novelId,
        name,
      };

      if (obj['gender'] != null) {
        request.gender = String(obj['gender']);
      }
      if (obj['description'] != null) {
        request.description = String(obj['description']);
      }
      if (obj['defaultVoiceId'] != null && obj['defaultVoiceId'] !== '') {
        const voiceId = Number(obj['defaultVoiceId']);
        if (!Number.isFinite(voiceId)) {
          throw new Error(`Phần tử #${i + 1}: defaultVoiceId không hợp lệ.`);
        }
        request.defaultVoiceId = voiceId;
      }

      return request;
    });
  }
}
