import React, { useEffect, useState } from "react";

import Spinner from "./Spinner";

const DelayedSpinner = () => {
    const [showSpinner, setShowSpinner] = useState(false);

    useEffect(() => {
        const timer = setTimeout(() => setShowSpinner(true), 300);

        return () => clearTimeout(timer);
    });

    return showSpinner && <Spinner />;
};

export default DelayedSpinner;
