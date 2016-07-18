import chai  from "chai";
var expect = chai.expect;
var assert = chai.assert;

import sinon  from "sinon";
import sinonChai  from "sinon-chai";
chai.use(sinonChai);
import React  from "react";
import $  from "jquery";

import RepositoryActions  from "actions/RepositoryActions";
import xor from "utils/xor";


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

    it("xor", function() {
        // a(0,0,0); a(1,1,1); a(1,0,0); a(0,1,0); a(0,0,1);
        assert(!xor(0,0,0));
        assert(!xor(1,1,1));

        assert(xor(0,0,1));
        assert(xor(0,1,0));
        assert(xor(1,0,0));
        assert(xor(1,1,0));
        assert(xor(0,1,1));
        assert(xor(1,0,1));
    });

});
