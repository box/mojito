import ReactGA from 'react-ga';

class GoogleAnalytics {

    enable(trackId, userId) {
        ReactGA.initialize(trackId, {
            gaOptions: {
                userId: userId
            }
        });

        this.enabled = true;
    }

    currentPageView() {
        if (this.enabled) {
            ReactGA.pageview(window.location.pathname + window.location.search);
        }
    }

    hash(string) {
        let hash = 0;

        for (let i = 0; i < string.length; i++) {
            let chr = string.charCodeAt(i);
            hash = ((hash << 5) - hash) + chr;
            hash |= 0;
        }

        return hash;
    }
}

export default new GoogleAnalytics();


