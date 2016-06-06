import chai  from "chai";
var expect = chai.expect;
import sinon  from "sinon";
import sinonChai  from "sinon-chai";
chai.use(sinonChai);
import React  from "react/addons";
import $  from "jquery";

import RepositoryActions  from "actions/RepositoryActions";

beforeEach(function() {
    sinon.spy($, "get");
});

afterEach(function() {
    $.get.restore();
});

describe("MojitoTests", function() {

    it("should pass", function() {
        RepositoryActions.getAllRepositories();
        expect($.get).to.have.been.calledOnce;
    });

});
