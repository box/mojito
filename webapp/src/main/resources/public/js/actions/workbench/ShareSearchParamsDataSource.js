import ShareSearchParamsModalActions from "./ShareSearchParamsModalActions.js";
import ClobStorageClient from "../../sdk/ClobStorageClient.js";

const ShareSearchParamsDataSource = {

    saveSearchParams: {
        remote(store, searchParams) {
            const promise = ClobStorageClient.saveClob(searchParams).then(function (uuid) {
                return uuid;
            });

            return promise;
        },
        success: ShareSearchParamsModalActions.saveSearchParamsSuccess,
        error: ShareSearchParamsModalActions.saveSearchParamsError,
    },
    getSearchParams: {
        remote(store, uuid) {
            const promise = ClobStorageClient.getClob(uuid).then(function (searchParams) {
                return {
                    uuid: uuid,
                    searchParams: searchParams
                };
            });

            return promise;
        },
        success: ShareSearchParamsModalActions.getSearchParamsSuccess,
        error: ShareSearchParamsModalActions.getSearchParamsError
    }
};

export default ShareSearchParamsDataSource;
