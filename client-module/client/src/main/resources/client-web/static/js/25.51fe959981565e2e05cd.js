webpackJsonp([25],{"0bPT":function(e,s,t){"use strict";Object.defineProperty(s,"__esModule",{value:!0});var a=t("LPk9"),n=t("FJop"),o=t("x47x"),i={data:function(){return{agentHash:this.$route.query.agentHash,myNodeInfo:[]}},components:{Back:a.a,Password:n.a},created:function(){},mounted:function(){this.getMyNodeInfo("/consensus/agent/"+this.$route.query.agentHash)},activated:function(){this.getMyNodeInfo("/consensus/agent/"+this.$route.query.agentHash)},destroyed:function(){},methods:{getMyNodeInfo:function(e){var s=this;this.$fetch(e).then(function(e){if(e.success){var t=new o.BigNumber(1e-8);e.data.deposit=parseFloat(t.times(e.data.deposit).toString()),e.data.totalDeposit=parseFloat(t.times(e.data.totalDeposit).toString()),s.myNodeInfo=e.data}})},closedNode:function(){var e=this;this.$fetch("/consensus/agent/stop/fee?address="+localStorage.getItem("newAccountAddress")).then(function(s){if(s.success){var t=new o.BigNumber(1e-8);e.$confirm(e.$t("message.c98")+" "+e.myNodeInfo.agentId+" "+e.$t("message.c99")+e.$t("message.miningFee")+t.times(s.data.value)+"NULS",e.$t("message.c86"),{confirmButtonText:e.$t("message.confirmButtonText"),cancelButtonText:e.$t("message.cancelButtonText")}).then(function(){"true"===localStorage.getItem("encrypted")?e.$refs.password.showPassword(!0):e.toSubmit("")}).catch(function(){e.$message({type:"waring",message:e.$t("message.c59"),duration:"1000"})})}else console.log("get fee err")})},toSubmit:function(e){var s=this,t={address:localStorage.getItem("newAccountAddress"),password:e};this.$post("/consensus/agent/stop",t).then(function(e){e.success?(s.$message({type:"success",message:s.$t("message.passWordSuccess")}),s.$router.push({name:"/consensus",params:{activeName:"first"}})):s.$message({type:"waring",message:s.$t("message.passWordFailed")+e.data.msg})})},toallPledge:function(){this.$router.push({path:"/consensus/allPledge",query:{agentName:this.myNodeInfo.agentId,txHash:this.myNodeInfo.txHash}})}},watch:{agentHash:function(e,s){console.log("agentHash: "+e,s),this.getMyNodeInfo("/consensus/agent/"+this.agentHash)}}},c={render:function(){var e=this,s=e.$createElement,t=e._self._c||s;return t("div",{staticClass:"node-info"},[t("Back",{attrs:{backTitle:this.$t("message.consensusManagement")}}),e._v(" "),t("h2",[e._v(e._s(this.myNodeInfo.agentId))]),e._v(" "),t("ul",[t("li",[t("label",[e._v(e._s(e.$t("message.c16")))]),t("span",[e._v(e._s(this.myNodeInfo.agentName?this.myNodeInfo.agentName:this.myNodeInfo.agentAddress))])]),e._v(" "),t("li",[t("label",[e._v(e._s(e.$t("message.state")))]),t("span",[e._v(" "+e._s(e.$t("message.status"+this.myNodeInfo.status)))])]),e._v(" "),t("li",[t("label",[e._v(e._s(e.$t("message.c25")))]),t("span",[e._v(e._s(this.myNodeInfo.deposit))])]),e._v(" "),t("li",[t("label",[e._v(e._s(e.$t("message.c17")))]),t("span",[e._v(e._s(this.myNodeInfo.commissionRate)+" %")])]),e._v(" "),t("li",[t("label",[e._v(e._s(e.$t("message.c18")))]),t("span",[e._v(e._s(this.myNodeInfo.creditVal))])]),e._v(" "),t("li",[t("label",[e._v(e._s(e.$t("message.c19")))]),t("span",[e._v(e._s(this.myNodeInfo.memberCount))])]),e._v(" "),t("li",[t("label",[e._v(e._s(e.$t("message.c1")))]),t("span",{staticClass:"cursor-p text-d",on:{click:e.toallPledge}},[e._v(e._s(this.myNodeInfo.totalDeposit))])])]),e._v(" "),t("el-button",{staticClass:"bottom-btn",attrs:{type:"button"},on:{click:e.closedNode}},[e._v(e._s(e.$t("message.c62")))]),e._v(" "),t("Password",{ref:"password",on:{toSubmit:e.toSubmit}})],1)},staticRenderFns:[]};var d=t("vSla")(i,c,!1,function(e){t("fVQ9")},null,null);s.default=d.exports},fVQ9:function(e,s){}});