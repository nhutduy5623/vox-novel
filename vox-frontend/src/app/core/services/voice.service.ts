import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, unwrapApiResponse } from '../models/api-response.model';
import { VoiceResponse } from '../models/voice.model';

@Injectable({ providedIn: 'root' })
export class VoiceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.coreContentApiUrl}/api/voices`;

  getAll(providerId?: number): Observable<VoiceResponse[]> {
    const url = providerId != null ? `${this.baseUrl}?providerId=${providerId}` : this.baseUrl;
    return this.http.get<ApiResponse<VoiceResponse[]>>(url).pipe(map(unwrapApiResponse));
  }
}
