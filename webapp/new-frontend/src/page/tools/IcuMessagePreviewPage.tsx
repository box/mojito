import './icu-message-preview-page.css';

import { useCallback } from 'react';

import { requestAiIcuParameterValues } from '../../api/ai-icu';
import { type IcuAiSuggestionRequest, IcuMessagePreview } from '../../components/IcuMessagePreview';

export function IcuMessagePreviewPage() {
  const handleRequestAiValues = useCallback(
    (request: IcuAiSuggestionRequest) =>
      requestAiIcuParameterValues({
        message: request.message,
        locale: request.locale,
        parameters: request.parameters,
        currentValues: request.values,
      }),
    [],
  );

  return (
    <div className="page-wrapper icu-message-preview-page">
      <div className="card card--padded">
        <div className="card__header">
          <div>
            <h1 className="page-title">ICU Message Preview</h1>
            <p className="hint">
              Parse ICU messages, infer parameters, test multiple value sets, and render concrete
              examples.
            </p>
          </div>
        </div>
        <div className="card__content card__content--stack">
          <IcuMessagePreview requestAiValues={handleRequestAiValues} />
        </div>
      </div>
    </div>
  );
}
