import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError } from '../../../core/models/api-response.model';
import {
  CreateCharacterRequest,
  GENDER_OPTIONS,
  NovelCharacterResponse,
} from '../../../core/models/character.model';
import { VoiceResponse } from '../../../core/models/voice.model';
import { CharacterService } from '../../../core/services/character.service';
import { playAudioUrl, voicePreviewUrl } from '../../../core/utils/audio.util';

@Component({
  selector: 'app-character-form-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './character-form-dialog.component.html',
  styleUrl: './character-form-dialog.component.css',
})
export class CharacterFormDialogComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);
  private readonly characterService = inject(CharacterService);

  @Input() open = false;
  @Input() novelId!: number;
  @Input() character: NovelCharacterResponse | null = null;
  @Input() voices: VoiceResponse[] = [];

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<NovelCharacterResponse>();

  readonly genderOptions = GENDER_OPTIONS;
  saving = false;
  errorMessage: string | null = null;

  readonly form = this.fb.group({
    name: ['', Validators.required],
    gender: ['MALE'],
    description: [''],
    defaultVoiceId: [''],
    isNarrator: [false],
  });

  get isEdit(): boolean {
    return this.character != null;
  }

  @Input() createForChapter = false;

  get dialogTitle(): string {
    if (this.isEdit) {
      return 'Sửa nhân vật';
    }
    return this.createForChapter ? 'Thêm nhân vật (truyện + chương)' : 'Thêm nhân vật';
  }

  get selectedVoicePreviewUrl(): string | null {
    return voicePreviewUrl(this.form.controls.defaultVoiceId.value, this.voices);
  }

  previewSelectedVoice(): void {
    const url = this.selectedVoicePreviewUrl;
    if (!url) {
      window.alert('Giọng này chưa có link preview.');
      return;
    }
    playAudioUrl(url);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue || changes['character']) {
      this.patchForm();
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-backdrop')) {
      this.close();
    }
  }

  close(): void {
    if (!this.saving) {
      this.closed.emit();
    }
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const voiceId = raw.defaultVoiceId ? Number(raw.defaultVoiceId) : undefined;
    const payload: CreateCharacterRequest = {
      novelId: raw.isNarrator ? 0 : this.novelId,
      name: raw.name!.trim(),
      gender: raw.gender ?? undefined,
      description: raw.description?.trim() || undefined,
      defaultVoiceId: voiceId,
    };

    this.saving = true;
    this.errorMessage = null;

    const req$ = this.isEdit
      ? this.characterService.update(this.character!.id, payload)
      : this.characterService.create(payload);

    req$.subscribe({
      next: (saved) => {
        this.saving = false;
        this.saved.emit(saved);
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Không thể lưu nhân vật.';
      },
    });
  }

  private patchForm(): void {
    if (this.character) {
      this.form.reset({
        name: this.character.name,
        gender: this.character.gender ?? 'MALE',
        description: this.character.description ?? '',
        defaultVoiceId: this.character.defaultVoiceId ? String(this.character.defaultVoiceId) : '',
        isNarrator: this.character.novelId === 0,
      });
    } else {
      this.form.reset({
        name: '',
        gender: 'MALE',
        description: '',
        defaultVoiceId: '',
        isNarrator: false,
      });
    }
    this.errorMessage = null;
  }
}
