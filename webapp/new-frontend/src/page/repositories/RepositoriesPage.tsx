import './repositories-page.css';

export function RepositoriesPage() {
  const repositories = [
    { name: 'Web App', rejected: 16, needsTranslation: 58, needsReview: 27 },
    { name: 'Mobile App', rejected: 6, needsTranslation: 16, needsReview: 8, selected: true },
    { name: 'Marketing Site', rejected: 0, needsTranslation: 0, needsReview: 0 },
    ...Array.from({ length: 47 }, (_, index) => ({
      name: `Repo ${index + 1}`,
      rejected: (index * 3) % 12,
      needsTranslation: (index * 7) % 20,
      needsReview: (index * 5) % 15,
    })),
  ];

  const locales = [
    { name: 'Spanish (Spain)', rejected: 5, needsTranslation: 12, needsReview: 6 },
    { name: 'Japanese', rejected: 1, needsTranslation: 4, needsReview: 2 },
    { name: 'French (France)', rejected: 0, needsTranslation: 5, needsReview: 2 },
    { name: 'German', rejected: 0, needsTranslation: 3, needsReview: 1 },
    { name: 'Portuguese (Brazil)', rejected: 0, needsTranslation: 6, needsReview: 3 },
    ...Array.from({ length: 45 }, (_, index) => ({
      name: `Locale ${index + 1}`,
      rejected: (index * 2) % 7,
      needsTranslation: (index * 3) % 12,
      needsReview: (index * 4) % 10,
    })),
  ];

  const formatCount = (value: number) => (value === 0 ? '' : value);

  return (
    <div className="repositories-page">
      <div className="repositories-page__section">
        <table className="repositories-page__table">
          <thead>
            <tr>
              <th className="repositories-page__header-cell--searchable">
                <span>Name</span>
                <input
                  type="search"
                  className="repositories-page__search"
                  placeholder="Search"
                  aria-label="Search repositories"
                />
              </th>
              <th className="repositories-page__header-cell--number">Rejected</th>
              <th className="repositories-page__header-cell--number">
                Needs translation
              </th>
              <th className="repositories-page__header-cell--number">Needs review</th>
            </tr>
          </thead>
          <tbody>
            {repositories.map((repo) => (
              <tr
                key={repo.name}
                className={
                  repo.selected
                    ? 'repositories-page__row repositories-page__row--selected'
                    : 'repositories-page__row'
                }
              >
                <td className="repositories-page__cell">{repo.name}</td>
                <td className="repositories-page__cell repositories-page__cell--number">
                  {formatCount(repo.rejected)}
                </td>
                <td className="repositories-page__cell repositories-page__cell--number">
                  {formatCount(repo.needsTranslation)}
                </td>
                <td className="repositories-page__cell repositories-page__cell--number">
                  {formatCount(repo.needsReview)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="repositories-page__divider"></div>
      <div className="repositories-page__section">
        <table className="repositories-page__table">
          <thead>
            <tr>
              <th>Locale</th>
              <th className="repositories-page__header-cell--number">Rejected</th>
              <th className="repositories-page__header-cell--number">
                Needs translation
              </th>
              <th className="repositories-page__header-cell--number">Needs review</th>
            </tr>
          </thead>
          <tbody>
            {locales.map((locale) => (
              <tr key={locale.name} className="repositories-page__row">
                <td className="repositories-page__cell">{locale.name}</td>
                <td className="repositories-page__cell repositories-page__cell--number">
                  {formatCount(locale.rejected)}
                </td>
                <td className="repositories-page__cell repositories-page__cell--number">
                  {formatCount(locale.needsTranslation)}
                </td>
                <td className="repositories-page__cell repositories-page__cell--number">
                  {formatCount(locale.needsReview)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
