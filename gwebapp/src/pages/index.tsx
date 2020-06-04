import * as React from "react"

const somefunc = () => {
    return <div>coucou this is great</div>
}


export default function Home() {
    return <>
        <div>Hello world!</div>
        {somefunc()}
    </>
}
