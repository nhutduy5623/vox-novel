import { NovelCharacterResponse } from '../models/character.model';
import { ScriptLineDto } from '../models/chapter.model';

export function resequenceScriptLines(lines: ScriptLineDto[]): ScriptLineDto[] {
  return lines.map((line, index) => ({
    ...line,
    sequence: index + 1,
  }));
}

export function resolveCharacterName(
  characterId: string,
  characters: NovelCharacterResponse[],
): string {
  const found = characters.find((c) => String(c.id) === String(characterId));
  return found?.name ?? `ID ${characterId}`;
}
