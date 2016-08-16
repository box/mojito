export default class Authority {
    constructor() {
        /** @type {Number} */
        this.id = 0;

        /** @type {String} */
        this.authority = "";
    }

    /**
     * @param {Object[]} jsonAuthorities
     * @return {[Authority]}
     */
    static toAuthorities(jsonAuthorities) {
        let authorities = null;
        if (jsonAuthorities) {
            authorities = [];
            for (let jsonAuth of jsonAuthorities) {
                authorities.push(Authority.toAuthority(jsonAuth));
            }
        }
        return authorities;
    }

    /**
     * @param {Object} jsonAuthority
     * @return {Authority}
     */
    static toAuthority(jsonAuthority) {
        let authority = new Authority();
        authority.id = jsonAuthority.id;
        authority.authority = jsonAuthority.authority;

        return authority;
    }
}

