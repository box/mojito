import React from "react";
import PropTypes from "prop-types";
import Locales from "../../utils/Locales";

class RepositoryLocaleDropdown extends React.Component {
    static propTypes = {
        onSelect: PropTypes.func.isRequired,
        selectedLocale: PropTypes.object.isRequired,
        localeOptions: PropTypes.arrayOf(
            PropTypes.shape({
                bcp47Tag: PropTypes.string,
                id: PropTypes.number
            })
        ).isRequired,
        defaultLocaleTag: PropTypes.string
    };

    constructor(props) {
        super(props);
        this.state = {
            searchTerm: "",
            isOpen: false,
        };
        this.dropdownRef = React.createRef();
    }

    componentDidMount() {
        document.addEventListener("mousedown", this.handleClickOutside);
        if (this.props.defaultLocaleTag) {
            const defaultLocale = this.getDefaultLocale(this.props.defaultLocaleTag);
            if (defaultLocale) {
                this.handleSelect(defaultLocale);
            }
        }
    }

    getDefaultLocale = (defaultLocaleTag) => {
        return this.props.localeOptions.find(locale => locale.bcp47Tag === defaultLocaleTag) || null;
    }

    getLocaleDisplayName = (bcp47Tag) => {
        return `${Locales.getDisplayName(bcp47Tag)} (${bcp47Tag})`;
    }

    componentWillUnmount() {
        document.removeEventListener("mousedown", this.handleClickOutside);
    }

    handleSelect = (locale) => {
        this.setState({ isOpen: false, searchTerm: this.getLocaleDisplayName(locale.bcp47Tag) });
        if (this.props.onSelect) {
            this.props.onSelect(locale);
        }
    };

    handleInputChange = (e) => {
        this.setState({ searchTerm: e.target.value, isOpen: true });
    };

    handleInputClick = () => {
        this.setState({ isOpen: true });
    };

    handleClickOutside = (event) => {
        if (
            this.dropdownRef.current &&
            !this.dropdownRef.current.contains(event.target)
        ) {
            this.setState(prevState => ({
                isOpen: false,
                searchTerm: prevState.searchTerm === "" && this.props.selectedLocale.bcp47Tag
                    ? this.getLocaleDisplayName(this.props.selectedLocale.bcp47Tag)
                    : prevState.searchTerm
            }));
        }
    };

    getInputValue = () => {
        if (this.state.isOpen) {
            return this.state.searchTerm;
        }
        if (this.props.selectedLocale && this.props.selectedLocale.bcp47Tag) {
            return this.getLocaleDisplayName(this.props.selectedLocale.bcp47Tag);
        }
        if (this.props.defaultLocale) {
            return this.getLocaleDisplayName(this.props.defaultLocale);
        }
        return "";
    };

    getFilteredLocales = () => {
        return this.props.localeOptions.filter(locale => {
            return this.getLocaleDisplayName(locale.bcp47Tag).toLowerCase().includes(this.state.searchTerm.toLowerCase());
        });
    };

    render() {
        const filteredLocales = this.getFilteredLocales();
        return (
            <div ref={this.dropdownRef} className="locale-dropdown-root">
                <input
                    type="text"
                    className="form-control locale-dropdown-input"
                    placeholder="Choose a locale"
                    value={this.getInputValue()}
                    onChange={this.handleInputChange}
                    onClick={this.handleInputClick}
                    autoComplete="off"
                />
                {this.state.isOpen && (
                    <ul
                        className="dropdown-menu locale-dropdown-menu"
                    >
                        {filteredLocales.length > 0 ? (
                            filteredLocales.map(locale => (
                                <li
                                    key={locale.id}
                                    className="locale-dropdown-item"
                                    onClick={() => this.handleSelect(locale)}
                                >
                                    <a>
                                        {this.getLocaleDisplayName(locale.bcp47Tag)}
                                    </a>
                                </li>
                            ))
                        ) : (
                            <li className="disabled locale-dropdown-item-disabled">
                                <a>No results</a>
                            </li>
                        )}
                    </ul>
                )}
            </div>
        );
    }
}

export default RepositoryLocaleDropdown;
