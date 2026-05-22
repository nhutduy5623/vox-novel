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
import { ScriptLineDto } from '../../../core/models/chapter.model';
import { NovelCharacterResponse } from '../../../core/models/character.model';

@Component({
  selector: 'app-script-line-dialog',
  imports: [ReactiveFormsModule],
  templateUrl: './script-line-dialog.component.html',
  styleUrl: './script-line-dialog.component.css',
})
export class ScriptLineDialogComponent implements OnChanges {
  private readonly fb = inject(FormBuilder);

  @Input() open = false;
  @Input() line: ScriptLineDto | null = null;
  @Input() characters: NovelCharacterResponse[] = [];

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<ScriptLineDto>();

  readonly form = this.fb.nonNullable.group({
    characterId: ['', Validators.required],
    text: ['', Validators.required],
    status: [''],
  });

  get dialogTitle(): string {
    return this.line ? 'Sửa dòng kịch bản' : 'Thêm dòng kịch bản';
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue || changes['line']) {
      this.patchForm();
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-backdrop')) {
      this.close();
    }
  }

  close(): void {
    this.closed.emit();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    this.saved.emit({
      sequence: this.line?.sequence ?? 0,
      characterId: v.characterId,
      text: v.text.trim(),
      audioDraftLink: this.line?.audioDraftLink ?? null,
      status: v.status.trim() || null,
    });
  }

  private patchForm(): void {
    if (this.line) {
      this.form.reset({
        characterId: this.line.characterId,
        text: this.line.text,
        status: this.line.status ?? '',
      });
    } else {
      const firstChar = this.characters[0];
      this.form.reset({
        characterId: firstChar ? String(firstChar.id) : '',
        text: '',
        status: '',
      });
    }
  }
}
