import { VoiceResponse } from '../models/voice.model';
import { NovelCharacterResponse } from '../models/character.model';
import { environment } from '../../../environments/environment';

let activeAudio: HTMLAudioElement | null = null;
let playbackQueue: string[] = [];
let currentQueueIndex = 0;
let onQueueFinished: (() => void) | null = null;

export function audioProxyUrl(minioUrl: string): string {
  if (!minioUrl) return '';
  const match = minioUrl.match(/voxnovel-audio\/(.+)/);
  if (match) {
    return `${environment.coreContentApiUrl}/api/media/audio/voxnovel-audio/${match[1]}`;
  }
  return minioUrl;
}

export function playAudioUrl(url: string): void {
  if (typeof window === 'undefined') {
    return;
  }
  stopSequentialPlayback();
  const proxiedUrl = audioProxyUrl(url);
  stopAudio();
  activeAudio = new Audio(proxiedUrl);
  activeAudio.play().catch(() => {
    window.alert('Không phát được audio. Kiểm tra URL hoặc quyền trình duyệt.');
  });
}

export function playSequentialAudio(urls: string[], onFinished?: () => void): void {
  if (typeof window === 'undefined' || urls.length === 0) {
    return;
  }
  stopSequentialPlayback();
  playbackQueue = urls.map(url => audioProxyUrl(url));
  currentQueueIndex = 0;
  onQueueFinished = onFinished || null;
  playNextInQueue();
}

function playNextInQueue(): void {
  if (currentQueueIndex >= playbackQueue.length) {
    if (onQueueFinished) {
      onQueueFinished();
    }
    return;
  }
  if (activeAudio) {
    activeAudio.pause();
    activeAudio = null;
  }
  activeAudio = new Audio(playbackQueue[currentQueueIndex]);
  activeAudio.onended = () => {
    currentQueueIndex++;
    playNextInQueue();
  };
  activeAudio.play().catch(() => {
    currentQueueIndex++;
    playNextInQueue();
  });
}

export function stopSequentialPlayback(): void {
  playbackQueue = [];
  currentQueueIndex = 0;
  if (onQueueFinished) {
    onQueueFinished = null;
  }
}

export function stopAudio(): void {
  if (activeAudio) {
    activeAudio.pause();
    activeAudio.currentTime = 0;
    activeAudio = null;
  }
  stopSequentialPlayback();
}

export function voicePreviewUrl(
  voiceId: number | string | null | undefined,
  voices: VoiceResponse[],
): string | null {
  if (voiceId == null || voiceId === '') {
    return null;
  }
  const id = Number(voiceId);
  const voice = voices.find((v) => v.id === id);
  return voice?.previewUrl?.trim() || null;
}

export function characterVoicePreviewUrl(
  character: NovelCharacterResponse,
  voices: VoiceResponse[],
): string | null {
  if (character.defaultVoiceId != null) {
    const fromVoice = voicePreviewUrl(character.defaultVoiceId, voices);
    if (fromVoice) {
      return audioProxyUrl(fromVoice);
    }
  }
  return null;
}
