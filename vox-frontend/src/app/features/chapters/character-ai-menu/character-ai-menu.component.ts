import { Component, Input, inject } from '@angular/core';
import { VoiceResponse } from '../../../core/models/voice.model';
import { VoiceService } from '../../../core/services/voice.service';
import {
  buildCharacterListAiPrompt,
  copyAiPrompt,
  openChatGptWithPrompt,
  openGeminiWithPrompt,
} from '../../../core/utils/ai-prompt.util';

@Component({
  selector: 'app-character-ai-menu',
  imports: [],
  templateUrl: './character-ai-menu.component.html',
  styleUrl: './character-ai-menu.component.css',
})
export class CharacterAiMenuComponent {
  private readonly voiceService = inject(VoiceService);

  @Input() novelId!: number;
  @Input() originalContent?: string;
  @Input() label = '✨ AI';

  menuOpen = false;
  loading = false;

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  private buildPrompt(voices: VoiceResponse[]): string {
    return buildCharacterListAiPrompt({
      novelId: this.novelId,
      voices,
      originalContent: this.originalContent,
    });
  }

  openChatGpt(): void {
    this.closeMenu();
    this.withPrompt((prompt) => openChatGptWithPrompt(prompt));
  }

  openGemini(): void {
    this.closeMenu();
    this.withPrompt((prompt) => void openGeminiWithPrompt(prompt));
  }

  copyPrompt(): void {
    this.closeMenu();
    this.withPrompt((prompt) => void copyAiPrompt(prompt));
  }

  private withPrompt(action: (prompt: string) => void): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.voiceService.getAll().subscribe({
      next: (voices) => {
        this.loading = false;
        action(this.buildPrompt(voices));
      },
      error: () => {
        this.loading = false;
        action(
          buildCharacterListAiPrompt({
            novelId: this.novelId,
            voices: [],
            originalContent: this.originalContent,
          }),
        );
      },
    });
  }
}
