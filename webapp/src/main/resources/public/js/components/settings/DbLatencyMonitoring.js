import React from "react";
import {Alert, Button, Col, ControlLabel, FormControl, FormGroup, Grid, HelpBlock, Row, Table} from "react-bootstrap";
import {FormattedMessage} from "react-intl";
import AuthorityService from "../../utils/AuthorityService";
import AdminMonitoringClient from "../../sdk/AdminMonitoringClient";

const DEFAULT_ITERATIONS = 5;
const MIN_ITERATIONS = 1;
const MAX_ITERATIONS = 20;

class DbLatencyMonitoring extends React.Component {
    state = {
        iterations: DEFAULT_ITERATIONS,
        loading: false,
        result: null,
        error: null,
    };

    componentDidMount() {
        if (AuthorityService.isAdmin()) {
            this.loadLatency();
        }
    }

    clampIterations(value) {
        if (isNaN(value)) {
            return DEFAULT_ITERATIONS;
        }
        return Math.max(MIN_ITERATIONS, Math.min(MAX_ITERATIONS, value));
    }

    handleIterationsChange = (event) => {
        const value = parseInt(event.target.value, 10);
        if (Number.isNaN(value)) {
            this.setState({ iterations: "" });
        } else {
            this.setState({ iterations: value });
        }
    };

    loadLatency = () => {
        if (!AuthorityService.isAdmin()) {
            return;
        }

        const iterations = this.clampIterations(Number(this.state.iterations));

        this.setState({ loading: true, error: null });

        AdminMonitoringClient.getDbLatency({ iterations })
            .then(result => {
                this.setState({ result, loading: false, iterations });
            })
            .catch(error => {
                // eslint-disable-next-line no-console
                console.error("Failed to load DB latency", error);
                this.setState({ error: error.message || "Request failed", loading: false });
            });
    };

    formatLatency(latency) {
        if (typeof latency !== "number" || Number.isNaN(latency)) {
            return "-";
        }
        return latency.toFixed(2);
    }

    renderSeries(labelId, defaultMessage, series) {
        if (!series) {
            return null;
        }

        return (
            <div className="mtl">
                <h4><FormattedMessage id={labelId} defaultMessage={defaultMessage}/></h4>
                <Table bordered condensed>
                    <tbody>
                        <tr>
                            <th><FormattedMessage id="monitoring.latency.min" defaultMessage="Min (ms)"/></th>
                            <td>{this.formatLatency(series.minLatencyMs)}</td>
                        </tr>
                        <tr>
                            <th><FormattedMessage id="monitoring.latency.max" defaultMessage="Max (ms)"/></th>
                            <td>{this.formatLatency(series.maxLatencyMs)}</td>
                        </tr>
                        <tr>
                            <th><FormattedMessage id="monitoring.latency.avg" defaultMessage="Average (ms)"/></th>
                            <td>{this.formatLatency(series.averageLatencyMs)}</td>
                        </tr>
                    </tbody>
                </Table>

                <h5><FormattedMessage id="monitoring.latency.measurements" defaultMessage="Measurements"/></h5>
                <Table striped bordered condensed>
                    <thead>
                        <tr>
                            <th><FormattedMessage id="monitoring.latency.iteration" defaultMessage="Iteration"/></th>
                            <th><FormattedMessage id="monitoring.latency.latency" defaultMessage="Latency (ms)"/></th>
                        </tr>
                    </thead>
                    <tbody>
                    {series.measurements.map(measurement => (
                        <tr key={measurement.iteration}>
                            <td>{measurement.iteration}</td>
                            <td>{this.formatLatency(measurement.latencyMs)}</td>
                        </tr>
                    ))}
                    </tbody>
                </Table>
            </div>
        );
    }

    renderResultSummary() {
        const { result } = this.state;
        if (!result) {
            return (
                <Alert bsStyle="info">
                    <FormattedMessage id="monitoring.latency.noData" defaultMessage="Run a measurement to see results."/>
                </Alert>
            );
        }

        const lastRun = new Date(result.timestamp).toLocaleString();

        return (
            <div>
                <h4><FormattedMessage id="monitoring.latency.overview" defaultMessage="Overview"/></h4>
                <Table bordered condensed>
                    <tbody>
                        <tr>
                            <th><FormattedMessage id="monitoring.latency.lastRun" defaultMessage="Last run"/></th>
                            <td>{lastRun}</td>
                        </tr>
                        <tr>
                            <th><FormattedMessage id="monitoring.latency.iterationsUsed" defaultMessage="Iterations used"/></th>
                            <td>{result.iterations}</td>
                        </tr>
                    </tbody>
                </Table>

                {this.renderSeries("monitoring.latency.series.raw", "Direct JDBC (select 1)", result.raw)}
                {this.renderSeries("monitoring.latency.series.hibernateHealth", "Hibernate (select 1)", result.hibernateHealth)}
                {this.renderSeries("monitoring.latency.series.hibernateRepo", "Hibernate repositories query", result.hibernateRepo)}
            </div>
        );
    }

    renderContent() {
        if (!AuthorityService.isAdmin()) {
            return (
                <Alert bsStyle="danger">
                    <FormattedMessage id="monitoring.latency.forbidden" defaultMessage="Only administrators can access this page."/>
                </Alert>
            );
        }

        return (
            <div>
                <Grid fluid>
                    <Row className="mbm">
                        <Col md={6}>
                            <form onSubmit={(event) => { event.preventDefault(); this.loadLatency(); }}>
                                <FormGroup controlId="iterations">
                                    <ControlLabel><FormattedMessage id="monitoring.latency.iterations" defaultMessage="Iterations"/></ControlLabel>
                                    <FormControl
                                        type="number"
                                        min={MIN_ITERATIONS}
                                        max={MAX_ITERATIONS}
                                        value={this.state.iterations}
                                        onChange={this.handleIterationsChange}
                                    />
                                    <HelpBlock>
                                        <FormattedMessage
                                            id="monitoring.latency.iterations.help"
                                            defaultMessage="Choose between {min} and {max} probes to average."
                                            values={{ min: MIN_ITERATIONS, max: MAX_ITERATIONS }}
                                        />
                                    </HelpBlock>
                                </FormGroup>
                                <Button
                                    bsStyle="primary"
                                    disabled={this.state.loading}
                                    onClick={this.loadLatency}
                                >
                                    {this.state.loading ?
                                        <FormattedMessage id="monitoring.latency.loading" defaultMessage="Measuring..."/>
                                        :
                                        <FormattedMessage id="monitoring.latency.measure" defaultMessage="Measure"/>
                                    }
                                </Button>
                            </form>
                        </Col>
                    </Row>
                    {this.state.error && (
                        <Alert bsStyle="danger">
                            <FormattedMessage id="monitoring.latency.error" defaultMessage="Failed to measure latency."/>
                            {this.state.error && <div className="mtm"><code>{this.state.error}</code></div>}
                        </Alert>
                    )}
                    {this.renderResultSummary()}
                </Grid>
            </div>
        );
    }

    render() {
        return (
            <div className="ptl">
                <h3 className="mbm"><FormattedMessage id="monitoring.latency.title" defaultMessage="Database Latency"/></h3>
                {this.renderContent()}
            </div>
        );
    }
}

export default DbLatencyMonitoring;
