import './repositories-page.css';

export type RepositoryRow = {
  id: number;
  name: string;
  rejected: number;
  needsTranslation: number;
  needsReview: number;
  selected: boolean;
};

export type LocaleRow = {
  id: number;
  name: string;
  rejected: number;
  needsTranslation: number;
  needsReview: number;
};

type Props = {
  repositories: RepositoryRow[];
  locales: LocaleRow[];
  searchValue: string;
  onSearchChange: (value: string) => void;
  onSelectRepository: (id: number) => void;
};

const formatCount = (value: number) => (value === 0 ? '' : value);

export function RepositoriesPageView({
  repositories,
  locales,
  searchValue,
  onSearchChange,
  onSelectRepository,
}: Props) {
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
                  value={searchValue}
                  onChange={(event) => onSearchChange(event.target.value)}
                />
              </th>
              <th className="repositories-page__header-cell--number">Rejected</th>
              <th className="repositories-page__header-cell--number">Needs translation</th>
              <th className="repositories-page__header-cell--number">Needs review</th>
            </tr>
          </thead>
          <tbody>
            {repositories.map((repo) => (
              <tr
                key={repo.id}
                className={
                  repo.selected
                    ? 'repositories-page__row repositories-page__row--selected'
                    : 'repositories-page__row'
                }
                onClick={() => onSelectRepository(repo.id)}
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
              <th className="repositories-page__header-cell--number">Needs translation</th>
              <th className="repositories-page__header-cell--number">Needs review</th>
            </tr>
          </thead>
          <tbody>
            {locales.map((locale) => (
              <tr key={locale.id} className="repositories-page__row">
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
