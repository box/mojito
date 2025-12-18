import { useCallback, useEffect, useRef, useState } from 'react';

import { WorkbenchPageView, type WorkbenchRow } from './WorkbenchPageView';

type MockTextUnit = {
  textUnitName: string;
  repositoryName: string;
  assetPath?: string;
  source: string;
  comment?: string;
  locales: Array<{
    locale: string;
    translation: string | null;
    status: string;
    comment?: string;
  }>;
};

const textUnitMocks: MockTextUnit[] = [
  {
    textUnitName: 'checkout.add_to_cart',
    repositoryName: 'checkout-service-monorepo-ios-android-web-with-really-long-name',
    assetPath: 'monorepo/apps/mobile/checkout/components/cta/strings/checkout/add_to_cart.ftl',
    source:
      'Add to cart and keep reviewing for later — this CTA appears on cards, quick cart, and PDP modals. Here is a whole extra sentence just to pad the mock data and make sure the textarea grows past four lines. And yet another sentence to push it even further beyond what we expect in production.',
    comment: 'Short CTA on PDP cards and quick cart; cap at roughly 22 characters.',
    locales: [
      {
        locale: 'fr-FR',
        translation:
          'Ajouter au panier et revenir plus tard — bouton court pour toutes les cartes produit.',
        status: 'Accepted',
      },
      {
        locale: 'de-DE',
        translation:
          'In den Warenkorb legen und später prüfen – Text lange genug pour recouvrir une Zeile complète.',
        status: 'To review',
        comment: 'Marketing asked for a punchier variant; pending approval.',
      },
      {
        locale: 'es-ES',
        translation:
          'Añadir al carrito ahora y vuelve cuando quieras (mensaje largo para probar el layout).',
        status: 'Accepted',
      },
      {
        locale: 'ja-JP',
        translation: null,
        status: 'To translate',
        comment: 'Prefer katakana for cart and keep the CTA on one line.',
      },
    ],
  },
  {
    textUnitName: 'billing.address.subtitle',
    repositoryName: 'billing-ui-and-accounting-shared-components',
    assetPath: 'services/billing/address/forms/flow_v2/long_version/forms.json',
    source:
      'Enter the billing details exactly as they appear on your payment statement so our partner bank can verify ownership. This paragraph intentionally runs long to mimic real legal copy. Keep the corrective tone clear.',
    comment: 'Legal copy. Keep the imperative tone and do not shorten "payment statement".',
    locales: [
      {
        locale: 'pt-BR',
        translation:
          'Informe os dados de cobrança exatamente como aparecem na sua fatura para que o banco parceiro confirme a titularidade. Texto longo com muitas palavras para ver o truncamento.',
        status: 'Accepted',
      },
      {
        locale: 'de-DE',
        translation:
          'Geben Sie die Rechnungsdaten exakt so ein, wie sie auf Ihrem Kontoauszug stehen, damit unsere Partnerbank den Besitz bestätigen kann. Auch dieser Text ist deliberately lang gezogene.',
        status: 'Accepted',
      },
      {
        locale: 'ja-JP',
        translation: null,
        status: 'To translate',
        comment: 'Should read naturally on a single line under the H4.',
      },
    ],
  },
  {
    textUnitName: 'Delete my workspace? (button label)',
    repositoryName: 'workspace-admin',
    assetPath: 'workspace/buttons/delete_workspace.properties',
    source: 'Delete my workspace',
    comment:
      'Legacy id matches the English copy. Keep the question mark where the locale requires it.',
    locales: [
      {
        locale: 'fr-FR',
        translation: 'Supprimer mon espace de travail ?',
        status: 'To review',
      },
      {
        locale: 'es-MX',
        translation: 'Eliminar mi espacio de trabajo',
        status: 'Accepted',
        comment: 'Reviewed with the CS team in Mexico City.',
      },
      { locale: 'pl-PL', translation: null, status: 'To translate' },
    ],
  },
  {
    textUnitName:
      "Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.',",
    repositoryName:
      'messaging-servicemessaging-servicemessaging-servicemessaging-servicemessaging-servicemessaging-service',
    assetPath:
      'notifications/digest/templates/notifications/digest/templates/notifications/digest/templates/notifications/digest/templates/notifications/digest/templates/notifications/digest/templates/body.ftl',
    source:
      'Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.',
    comment:
      'Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.Leave {summary} in English.\nTwo sentences max.',
    locales: [
      {
        locale: 'es-ES',
        translation:
          'Esto es lo que tu equipo publicó esta semana: {summary}. Mantén el texto por debajo de 500 caracteres para que quepa en la bandeja dentro del producto y asegúrate de conservar los marcadores {summary}. Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.Here is what your team shipped this week: {summary}. Keep this under 500 characters so it fits inside the in-product inbox preview. These sentences stretch out to test how the source column handles paragraphs.',
        status: 'To review',
      },
      {
        locale: 'pt-BR',
        translation:
          'Confira o que sua equipe entregou esta semana: {summary}. Evite passar de 500 caracteres para caber no inbox dentro do produto. Texto propositalmente longo para testar o layout.',
        status: 'Accepted',
      },
      {
        locale: 'de-DE',
        translation:
          'Das hat dein Team diese Woche ausgeliefert: {summary}. Bleib unter 500 Zeichen, damit es in der Inbox-Vorschau bleibt, auch wenn der Satz sehr lang wirkt.',
        status: 'Accepted',
      },
    ],
  },
  {
    textUnitName: 'mobile.offline_mode.tooltip',
    repositoryName: 'mobile-shell',
    assetPath: 'mobile/offline_mode/tooltip.json',
    source:
      'We cache the last 20 conversations on your device so you can keep reviewing on the subway or on spotty flights.',
    comment: 'Tooltip copy. Keep the casual tone.',
    locales: [
      {
        locale: 'fr-FR',
        translation:
          'Nous mettons en cache les 20 dernières conversations sur votre appareil pour que vous puissiez continuer à relire dans le métro.',
        status: 'Accepted',
      },
      {
        locale: 'it-IT',
        translation:
          'Memorizziamo sul dispositivo le ultime 20 conversazioni così puoi continuare a rivederle anche con connessione ballerina.',
        status: 'Accepted',
      },
      {
        locale: 'id-ID',
        translation:
          'Kami menyimpan 20 percakapan terakhir di perangkat agar kamu bisa terus meninjau meski koneksi kereta atau pesawat buruk.',
        status: 'Rejected',
        comment: 'Too informal per review; needs rewrite.',
      },
    ],
  },
  {
    textUnitName: 'We kept your filters right where you left them (banner title)',
    repositoryName: 'search-experience',
    assetPath: 'search/banners/return_filters.json',
    source:
      'We kept your filters right where you left them so you can jump back in and continue reviewing this long stream of mock data without losing context.',
    comment: 'Yes, the ID mirrors the English copy. Keep it playful but short.',
    locales: [
      {
        locale: 'en-GB',
        translation: 'We left your filters exactly where you needed them, so dive back in.',
        status: 'Accepted',
      },
      {
        locale: 'sv-SE',
        translation: 'Vi lät filtren ligga kvar där du lämnade dem så att du kan fortsätta direkt.',
        status: 'To review',
      },
      {
        locale: 'nl-NL',
        translation: 'We hebben je filters precies laten staan zodat je meteen weer kunt doorgaan.',
        status: 'Accepted',
      },
      {
        locale: 'ja-JP',
        translation:
          '前回のフィルター設定をそのまま残しておきました。すぐにレビューを再開できます。',
        status: 'Accepted',
      },
    ],
  },
  {
    textUnitName: 'settings.security.passkey_paragraph',
    repositoryName: 'settings-web',
    assetPath: 'settings/security/passkey.md',
    source:
      'Use a passkey instead of a password so you can sign in with Face ID, Windows Hello, or the security chip built into your device.',
    comment: 'Long form copy; translators can split it into two sentences if needed.',
    locales: [
      {
        locale: 'es-ES',
        translation:
          'Usa una passkey en lugar de una contraseña para iniciar sesión con Face ID, Windows Hello o el chip de seguridad integrado en tu dispositivo.',
        status: 'Accepted',
      },
      {
        locale: 'ko-KR',
        translation:
          '암호 대신 패스키를 사용하면 Face ID, Windows Hello 또는 기기에 내장된 보안 칩으로 로그인할 수 있습니다.',
        status: 'Accepted',
      },
      { locale: 'cs-CZ', translation: null, status: 'To translate' },
    ],
  },
  {
    textUnitName: 'support.timeline.comment',
    repositoryName: 'support-portal',
    assetPath: 'support/timeline/comment.md',
    source:
      '@{agent} left this note for the next shift. Keep it short and professional because customers can see it when we forward the ticket.',
    comment: 'Leave @{agent} intact. Sentence case only.',
    locales: [
      {
        locale: 'en-GB',
        translation:
          '@{agent} left this note for the next shift. Keep it short and professional because customers may see it when we forward the ticket.',
        status: 'Accepted',
      },
      {
        locale: 'fr-FR',
        translation:
          '@{agent} a laissé cette note pour l’équipe suivante. Restez concis et professionnel car le client peut la voir quand on transfère le ticket.',
        status: 'Accepted',
      },
      {
        locale: 'es-CL',
        translation: null,
        status: 'To translate',
        comment: 'Need Chile-specific register and verb choices.',
      },
    ],
  },
  {
    textUnitName: 'release_notes.rollout_eta',
    repositoryName: 'release-hub',
    assetPath: 'release_notes/rollout_eta.ftl',
    source:
      'Phase {phaseNumber} is rolling out on {date}. Keep stakeholders in the loop from Control Center.',
    comment: 'Keep {phaseNumber} and {date} placeholders as-is.',
    locales: [
      {
        locale: 'de-DE',
        translation:
          'Phase {phaseNumber} wird am {date} ausgerollt. Informiere Stakeholder direkt aus dem Control Center.',
        status: 'To review',
      },
      {
        locale: 'es-ES',
        translation:
          'La fase {phaseNumber} se lanza el {date}. Mantén informados a los interesados desde el Control Center.',
        status: 'Accepted',
      },
      {
        locale: 'it-IT',
        translation:
          'La fase {phaseNumber} verrà distribuita il {date}. Tieni aggiornate le parti interessate dal Control Center.',
        status: 'Accepted',
      },
    ],
  },
  {
    textUnitName: 'email.footer.contact_address',
    repositoryName: 'system-email',
    assetPath: 'system_email/footer/contact_address.ftl',
    source:
      'You are receiving this message because you manage the Mojito workspace. Questions? Contact us at 77 Battery Street, San Francisco, CA.',
    comment: 'Legal footer. Keep punctuation identical and do not translate "Mojito".',
    locales: [
      {
        locale: 'en-GB',
        translation:
          'You are receiving this message because you manage the Mojito workspace. Questions? Contact us at 77 Battery Street, San Francisco, CA.',
        status: 'Accepted',
      },
      {
        locale: 'fr-FR',
        translation:
          'Vous recevez ce message parce que vous administrez l’espace de travail Mojito. Des questions ? Contactez-nous au 77 Battery Street, San Francisco, CA.',
        status: 'Accepted',
      },
      {
        locale: 'de-CH',
        translation: null,
        status: 'To translate',
        comment: 'Need Swiss-specific address abbreviations.',
      },
    ],
  },
  {
    textUnitName:
      'We kept your filters right where you left them so you can jump back in (banner title)',
    repositoryName: 'ios-app-shell',
    assetPath: 'ios/banners/return_filters.strings',
    source:
      'We kept your filters right where you left them so you can jump back in and continue reviewing this long stream of mock data without losing context.',
    comment: 'Legacy iOS ids mirrored the English copy; strings team is cleaning them up.',
    locales: [
      {
        locale: 'en-GB',
        translation: 'We left your filters exactly where they were so you can dive back in.',
        status: 'Accepted',
      },
      {
        locale: 'fr-FR',
        translation:
          'Nous avons laissé vos filtres exactement là où vous les aviez pour que vous puissiez reprendre immédiatement.',
        status: 'To review',
        comment: 'Need to ensure it doesn’t wrap awkwardly on iPhone SE width.',
      },
      {
        locale: 'ja-JP',
        translation: null,
        status: 'To translate',
        comment: 'Keep the tone light; string id already matches the English source.',
      },
    ],
  },
  {
    textUnitName: 'messaging.payments.pay_later_cta',
    repositoryName: 'messaging-platform',
    assetPath: 'messaging/payments/pay_later_cta.ftl',
    source: 'Pay in two installments with no extra fees and keep your order moving.',
    comment: 'CTA in chat – keep it concise and reassuring.',
    locales: [
      {
        locale: 'ar-SA',
        translation: 'ادفع على دفعتين من دون رسوم إضافية وواصل معالجة الطلب.',
        status: 'To review',
      },
      {
        locale: 'he-IL',
        translation: 'שלם בשתי פעימות בלי עמלות ונמשיך עם ההזמנה שלך.',
        status: 'Accepted',
      },
      {
        locale: 'fa-IR',
        translation: null,
        status: 'To translate',
        comment: 'Needs RTL test; keep the reassuring tone.',
      },
      {
        locale: 'ur-PK',
        translation: 'دو آسان اقساط میں ادائیگی کریں، بغیر اضافی فیس کے، اور آرڈر جاری رکھیں۔',
        status: 'Accepted',
      },
    ],
  },
];

const statusOptions = ['Accepted', 'To review', 'To translate', 'Rejected'];

const mockRows: WorkbenchRow[] = textUnitMocks.flatMap((unit) =>
  unit.locales.map((localeVariant) => ({
    id: `${unit.textUnitName}:${localeVariant.locale}`,
    textUnitName: unit.textUnitName,
    repositoryName: unit.repositoryName,
    assetPath: unit.assetPath ?? null,
    locale: localeVariant.locale,
    source: unit.source,
    translation: localeVariant.translation,
    status: localeVariant.status,
    comment: localeVariant.comment ?? unit.comment ?? null,
  })),
);

export function WorkbenchPage() {
  const [rows, setRows] = useState(mockRows);
  const [editingRowId, setEditingRowId] = useState<string | null>(null);
  const [editingValue, setEditingValue] = useState('');
  const translationInputRef = useRef<HTMLTextAreaElement | null>(null);
  const rowRefs = useRef<Record<string, HTMLDivElement | null>>({});

  const registerRowRef = useCallback((rowId: string, element: HTMLDivElement | null) => {
    rowRefs.current[rowId] = element;
  }, []);

  const handleStartEditing = useCallback((rowId: string, translation: string | null) => {
    setEditingRowId(rowId);
    setEditingValue(translation ?? '');
  }, []);

  const handleCancelEditing = useCallback(() => {
    setEditingRowId(null);
    setEditingValue('');
  }, []);

  const handleChangeEditingValue = useCallback((value: string) => {
    setEditingValue(value);
  }, []);

  const handleSaveEditing = useCallback(() => {
    if (!editingRowId) {
      return;
    }
    setRows((previousRows) =>
      previousRows.map((row) =>
        row.id === editingRowId
          ? {
              ...row,
              translation: editingValue === '' ? null : editingValue,
              status: 'Accepted',
            }
          : row,
      ),
    );
    handleCancelEditing();
  }, [editingRowId, editingValue, handleCancelEditing]);

  const handleChangeStatus = useCallback((rowId: string, status: string) => {
    setRows((previousRows) =>
      previousRows.map((row) => (row.id === rowId ? { ...row, status } : row)),
    );
  }, []);

  useEffect(() => {
    if (!editingRowId) {
      return;
    }

    const rowElement = rowRefs.current[editingRowId];
    if (rowElement) {
      rowElement.scrollIntoView({ block: 'nearest' });
    }

    if (translationInputRef.current && document.activeElement !== translationInputRef.current) {
      translationInputRef.current.focus();
    }
  }, [editingRowId]);

  return (
    <WorkbenchPageView
      rows={rows}
      editingRowId={editingRowId}
      editingValue={editingValue}
      onStartEditing={handleStartEditing}
      onCancelEditing={handleCancelEditing}
      onSaveEditing={handleSaveEditing}
      onChangeEditingValue={handleChangeEditingValue}
      onChangeStatus={handleChangeStatus}
      statusOptions={statusOptions}
      translationInputRef={translationInputRef}
      registerRowRef={registerRowRef}
    />
  );
}
