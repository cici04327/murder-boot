import{_ as Ge,u as Ke,r as D,a as Qe,B as K,C as Xe,m as Ye,D as Ze,G as Je,c as p,n as v,p as f,e as t,w as l,g as m,j as et,i as tt,o as a,l as d,d as o,F as L,A as I,t as u,h as g,W as st,X as ue,Y as lt,Z as de,K as oe,$ as ce,U as ot,y as nt,a0 as it,a1 as at,a2 as rt,Q as ge,I as ut,a3 as pe,N as dt,a4 as ct,v as Q,a5 as gt,a6 as pt,a7 as vt,f as ft,a8 as mt,E as w}from"./index-Ce5KY3go.js";import{b as _t,c as ht,d as yt,e as wt}from"./store-B_20bsGh.js";import{i as kt}from"./script-16GduTyg.js";import{r as ve}from"./recommendation-bxl47vLr.js";import{r as xt,a as Dt,b as fe}from"./location-D9s35Imy.js";import{g as bt,L as Ct}from"./store-covers-D3nRp-u8.js";const Mt={class:"store-detail-container"},St={class:"store-header"},Lt={class:"store-images"},It=["src","alt"],Tt={class:"store-basic-info"},Vt={class:"store-rating"},zt={class:"review-count"},Rt={class:"address-with-distance"},$t={class:"store-tags"},Ut={class:"action-buttons"},Nt={class:"card-header"},At={key:0,class:"store-highlights"},Ft={class:"section-title"},Wt={class:"highlight-tags"},Bt={key:1,class:"store-facilities"},Ht={class:"section-title"},Pt={class:"facility-item"},Et={class:"store-description-section"},qt={class:"section-title"},jt=["innerHTML"],Ot={class:"store-business-info"},Gt={class:"section-title"},Kt={class:"info-card"},Qt={class:"info-icon"},Xt={class:"info-content"},Yt={class:"info-value"},Zt={class:"info-card"},Jt={class:"info-icon"},es={class:"info-content"},ts={class:"info-value"},ss={class:"info-card"},ls={class:"info-icon"},os={class:"info-content"},ns={class:"info-value"},is={class:"info-card"},as={class:"info-icon"},rs={class:"info-content"},us={class:"info-value"},ds={key:2,class:"store-environment"},cs={class:"section-title"},gs=["onClick"],ps=["src","alt"],vs={class:"image-overlay"},fs={class:"card-header"},ms={class:"room-header"},_s={class:"room-info"},hs={class:"info-item"},ys={key:0,class:"info-item"},ws={key:1,class:"info-item"},ks={key:0,class:"room-action"},xs={class:"card-header"},Ds={key:0,style:{"text-align":"center",padding:"20px",color:"rgba(255,255,255,0.5)"}},bs={key:1,style:{"text-align":"center",padding:"20px",color:"rgba(255,255,255,0.5)"}},Cs={key:2,class:"store-schedule-grid"},Ms={class:"ss-date"},Ss={class:"ss-time"},Ls={key:0,class:"ss-script"},Is={key:3,style:{"text-align":"center","margin-top":"16px"}},Ts={class:"card-header"},Vs={class:"reviews-list"},zs={key:0,class:"review-tip"},Rs={class:"review-header"},$s={class:"review-user"},Us={class:"username"},Ns={class:"review-date"},As={class:"review-content"},Fs={key:0,class:"review-images"},Ws={__name:"detail",setup(Bs){const T=et(),E=tt(),X=Ke(),q=D(!1),n=D(null),R=D([]),V=D([]),A=D(!1),F=D(""),Y=D(!1),b=D(""),Z=D(!1),J=D(null),z=D([]),j=D(!1),_=Qe({rating:5,content:"",images:[],imageList:[]}),M=K(()=>{var N,G;if(!((N=n.value)!=null&&N.openTime)||!((G=n.value)!=null&&G.closeTime))return null;const s=new Date,[e,r]=n.value.openTime.split(":").map(Number),[h,se]=n.value.closeTime.split(":").map(Number),$=s.getHours()*60+s.getMinutes(),x=e*60+r,U=h*60+se,c=$>=x&&$<U,k=U-$,W=x-$;return c?k<=60?{open:!0,urgent:!0,text:`营业中 · ${k}分钟后闭店`}:{open:!0,urgent:!1,text:"营业中"}:W>0&&W<=60?{open:!1,soon:!0,text:`即将开店 · ${W}分钟后`}:{open:!1,soon:!1,text:"已闭店"}}),ne=K(()=>R.value.filter(s=>s.status===1).length),O=K(()=>{var s;return(s=n.value)!=null&&s.reviewCount&&n.value.reviewCount>0?n.value.reviewCount:V.value.length}),me=K(()=>{var s,e;return(s=n.value)!=null&&s.coverImage?[n.value.coverImage]:(e=n.value)!=null&&e.name?[bt(n.value.name)]:["https://dummyimage.com/800x400/cccccc/666666&text=Store"]}),_e=(s="本店")=>`
    <div class="store-intro">
      <h5>🎭 关于我们</h5>
      <p><strong>${s}</strong>是一家专业的沉浸式剧本杀体验馆。我们致力于为每一位玩家打造难忘的推理之旅，用心呈现每一个故事，让您在虚拟与现实的交织中，体验不一样的人生。</p>
    </div>
    
    <div class="store-intro">
      <h5>🎭 专业团队</h5>
      <p>我们拥有<strong>10余名全职DM</strong>，平均从业经验<strong>3年以上</strong>，对每个剧本都有深入研究。每位DM都经过严格培训，掌握多种主持风格，无论是本格推理还是情感沉浸，都能为玩家提供极致的游戏体验。</p>
      <ul>
        <li>✨ 专业DM培训体系，持证上岗</li>
        <li>✨ 定期剧本研讨会，精进主持技巧</li>
        <li>✨ 个性化服务，根据玩家喜好推荐剧本</li>
        <li>✨ 全程跟进，及时解答疑问，调节游戏节奏</li>
      </ul>
    </div>
    
    <div class="store-intro">
      <h5>📚 海量剧本库</h5>
      <p>门店现有<strong>200+优质剧本</strong>，涵盖本格推理、情感沉浸、恐怖惊悚、欢乐互动、机制硬核、还原阵营等多种类型，<strong>每月更新10+新本</strong>，紧跟市场潮流。</p>
      <p><strong>热门剧本推荐：</strong></p>
      <ul>
        <li>🔍 <strong>本格推理：</strong>《东方快车谋杀案》《无人生还》《罗生门》等经典本格</li>
        <li>💔 <strong>情感沉浸：</strong>《雾中回忆》《时光代理人》《余生请多指教》</li>
        <li>👻 <strong>恐怖惊悚：</strong>《午夜出租车》《诡宅》《死亡循环》</li>
        <li>😄 <strong>欢乐互动：</strong>《饭局狼人杀》《谁是卧底》《剧本杀派对》</li>
        <li>⚙️ <strong>机制硬核：</strong>《迷雾侦探社》《密室逃脱》《犯罪现场》</li>
      </ul>
    </div>
    
    <div class="store-intro">
      <h5>🏠 豪华环境设施</h5>
      <p>门店总面积<strong>800㎡</strong>，共设<strong>6间独立主题房间</strong>，每间房都经过专业设计师精心打造，主题风格各异，氛围感十足。</p>
      <ul>
        <li>🎬 <strong>沉浸式场景：</strong>民国风、古风、现代都市、科幻未来等多种风格</li>
        <li>🎵 <strong>专业设备：</strong>高品质音响系统、智能灯光控制、投影设备</li>
        <li>🎨 <strong>精美道具：</strong>定制化道具，还原剧本场景，增强沉浸感</li>
        <li>🌡️ <strong>舒适体验：</strong>中央空调、新风系统，四季恒温</li>
        <li>📸 <strong>拍照打卡：</strong>多个精美场景，适合拍照留念</li>
      </ul>
    </div>
    
    <div class="store-intro">
      <h5>🎯 贴心服务</h5>
      <p>我们深知每一个细节都关乎玩家的体验，因此在服务上精益求精。</p>
      <ul>
        <li>☕ <strong>免费饮品：</strong>咖啡、茶水、果汁、零食无限量供应</li>
        <li>🛋️ <strong>舒适休息区：</strong>宽敞的等候大厅，提供舒适沙发和娱乐设施</li>
        <li>📱 <strong>高速WiFi：</strong>全馆覆盖高速无线网络</li>
        <li>🔌 <strong>充电设施：</strong>每个房间配备充电插座</li>
        <li>🎁 <strong>会员福利：</strong>积分兑换、生日优惠、专属活动</li>
        <li>📦 <strong>物品寄存：</strong>提供免费物品寄存服务</li>
        <li>🚻 <strong>卫生设施：</strong>干净整洁的卫生间，定时消毒</li>
      </ul>
    </div>
    
    <div class="store-intro">
      <h5>🚇 交通便利</h5>
      <p><strong>地铁：</strong>地铁10号线三里屯站A出口步行5分钟即达</p>
      <p><strong>公交：</strong>113路、115路、406路、416路等多路公交直达</p>
      <p><strong>自驾：</strong>门店附近有多个停车场，停车便利（可提供停车券）</p>
      <p><strong>周边配套：</strong>三里屯商圈，美食、购物、娱乐应有尽有</p>
    </div>
    
    <div class="store-intro">
      <h5>⭐ 口碑见证</h5>
      <p>自开业以来，我们累计服务<strong>5000+人次</strong>，用户好评率<strong>98%</strong>，是北京地区评分最高的剧本杀门店之一。</p>
      <ul>
        <li>🏆 2022年度"最受欢迎剧本杀门店"</li>
        <li>🏆 2023年度"最佳服务质量奖"</li>
        <li>🏆 大众点评五星商户</li>
        <li>🏆 美团必吃榜推荐商家</li>
      </ul>
      <p><em>"环境超棒，DM专业，剧本丰富，每次来都有新体验！"</em> - 来自会员@推理狂魔</p>
      <p><em>"朋友聚会的首选，氛围好服务好，强烈推荐！"</em> - 来自会员@剧本杀爱好者</p>
    </div>
    
    <div class="store-intro">
      <h5>🎉 特色活动</h5>
      <ul>
        <li>🎭 <strong>主题活动日：</strong>每月举办主题派对，剧本联动，惊喜不断</li>
        <li>🎓 <strong>新手专场：</strong>每周末开设新手专场，DM手把手教学</li>
        <li>💰 <strong>团购优惠：</strong>3人及以上享团购价，6人车本更优惠</li>
        <li>🎂 <strong>生日专属：</strong>生日当月游戏享8折优惠，还有神秘礼物</li>
        <li>🎁 <strong>会员福利：</strong>充值送优惠券，积分兑换剧本和周边</li>
      </ul>
    </div>
    
    <div class="store-intro store-contact">
      <h5>📞 联系我们</h5>
      <p>营业时间：10:00 - 22:00（全年无休）</p>
      <p>预约电话：010-12345678</p>
      <p>客服微信：探案密室官方客服</p>
      <p>官方微信公众号：探案密室</p>
      <p>门店地址：北京市朝阳区三里屯路xx号</p>
    </div>
    
    <div class="store-tips">
      <h5>💡 温馨提示</h5>
      <ul>
        <li>📅 建议提前1-3天预约，周末及节假日请尽早预约</li>
        <li>⏰ 请提前15分钟到店，以便DM讲解规则</li>
        <li>👥 部分剧本有人数要求，拼车可联系客服</li>
        <li>🎒 游戏过程中请遵守规则，爱护道具和设施</li>
        <li>📸 拍照留念请关闭闪光灯，避免影响他人体验</li>
      </ul>
    </div>
  `,ee=async()=>{q.value=!0;try{const s=await _t(T.params.id);if(console.log("门店详情响应:",s),s.data){if(n.value=s.data,n.value.tags=n.value.tags||["环境优雅","服务专业","交通便利"],n.value.images){const e=n.value.images.split(",").map(r=>r.trim()).filter(r=>r);e.length>0&&(n.value.coverImage=n.value.coverImage||e[0],n.value.environmentImages=n.value.environmentImages||e)}if(n.value.description||(n.value.description=_e(n.value.name)),X.isLoggedIn){J.value=Date.now();try{await ve(2,n.value.id,0)}catch(e){console.error("保存门店浏览历史失败:",e)}}}}catch(s){console.error("加载门店详情失败:",s),n.value={id:T.params.id,name:"探案密室·沉浸式剧本体验馆",address:"北京市朝阳区三里屯路19号",phone:"010-12345678",openTime:"10:00",closeTime:"22:00",rating:4.8,reviewCount:128,serviceCount:"5000+",scriptCount:"200+",tags:["环境优雅","服务专业","交通便利"],highlights:["5年老店·口碑保证","专业DM团队·剧情还原度高","海量剧本·更新快","豪华装修·沉浸式体验","地铁直达·交通便利","免费饮品·舒适环境"],facilities:["WiFi","空调","饮品","零食","停车场","独立包厢","投影设备","音响系统","桌游区","休息区","卫生间","充电设施"],description:`
        <div class="store-intro">
          <h5>🎭 关于我们</h5>
          <p><strong>探案密室</strong>成立于2019年，是北京地区知名的沉浸式剧本杀体验馆。我们致力于为每一位玩家打造难忘的推理之旅，用心呈现每一个故事，让您在虚拟与现实的交织中，体验不一样的人生。</p>
        </div>
        
        <div class="store-intro">
          <h5>🎭 专业团队</h5>
          <p>我们拥有<strong>10余名全职DM</strong>，平均从业经验<strong>3年以上</strong>，对每个剧本都有深入研究。每位DM都经过严格培训，掌握多种主持风格，无论是本格推理还是情感沉浸，都能为玩家提供极致的游戏体验。</p>
          <ul>
            <li>✨ 专业DM培训体系，持证上岗</li>
            <li>✨ 定期剧本研讨会，精进主持技巧</li>
            <li>✨ 个性化服务，根据玩家喜好推荐剧本</li>
            <li>✨ 全程跟进，及时解答疑问，调节游戏节奏</li>
          </ul>
        </div>
        
        <div class="store-intro">
          <h5>📚 海量剧本库</h5>
          <p>门店现有<strong>200+优质剧本</strong>，涵盖本格推理、情感沉浸、恐怖惊悚、欢乐互动、机制硬核、还原阵营等多种类型，<strong>每月更新10+新本</strong>，紧跟市场潮流。</p>
          <p><strong>热门剧本推荐：</strong></p>
          <ul>
            <li>🔍 <strong>本格推理：</strong>《东方快车谋杀案》《无人生还》《罗生门》等经典本格</li>
            <li>💔 <strong>情感沉浸：</strong>《雾中回忆》《时光代理人》《余生请多指教》</li>
            <li>👻 <strong>恐怖惊悚：</strong>《午夜出租车》《诡宅》《死亡循环》</li>
            <li>😄 <strong>欢乐互动：</strong>《饭局狼人杀》《谁是卧底》《剧本杀派对》</li>
            <li>⚙️ <strong>机制硬核：</strong>《迷雾侦探社》《密室逃脱》《犯罪现场》</li>
          </ul>
        </div>
        
        <div class="store-intro">
          <h5>🏠 豪华环境设施</h5>
          <p>门店总面积<strong>800㎡</strong>，共设<strong>6间独立主题房间</strong>，每间房都经过专业设计师精心打造，主题风格各异，氛围感十足。</p>
          <ul>
            <li>🎬 <strong>沉浸式场景：</strong>民国风、古风、现代都市、科幻未来等多种风格</li>
            <li>🎵 <strong>专业设备：</strong>高品质音响系统、智能灯光控制、投影设备</li>
            <li>🎨 <strong>精美道具：</strong>定制化道具，还原剧本场景，增强沉浸感</li>
            <li>🌡️ <strong>舒适体验：</strong>中央空调、新风系统，四季恒温</li>
            <li>📸 <strong>拍照打卡：</strong>多个精美场景，适合拍照留念</li>
          </ul>
        </div>
        
        <div class="store-intro">
          <h5>🎯 贴心服务</h5>
          <p>我们深知每一个细节都关乎玩家的体验，因此在服务上精益求精。</p>
          <ul>
            <li>☕ <strong>免费饮品：</strong>咖啡、茶水、果汁、零食无限量供应</li>
            <li>🛋️ <strong>舒适休息区：</strong>宽敞的等候大厅，提供舒适沙发和娱乐设施</li>
            <li>📱 <strong>高速WiFi：</strong>全馆覆盖高速无线网络</li>
            <li>🔌 <strong>充电设施：</strong>每个房间配备充电插座</li>
            <li>🎁 <strong>会员福利：</strong>积分兑换、生日优惠、专属活动</li>
            <li>📦 <strong>物品寄存：</strong>提供免费物品寄存服务</li>
            <li>🚻 <strong>卫生设施：</strong>干净整洁的卫生间，定时消毒</li>
          </ul>
        </div>
        
        <div class="store-intro">
          <h5>🚇 交通便利</h5>
          <p><strong>地铁：</strong>地铁10号线三里屯站A出口步行5分钟即达</p>
          <p><strong>公交：</strong>113路、115路、406路、416路等多路公交直达</p>
          <p><strong>自驾：</strong>门店附近有多个停车场，停车便利（可提供停车券）</p>
          <p><strong>周边配套：</strong>三里屯商圈，美食、购物、娱乐应有尽有</p>
        </div>
        
        <div class="store-intro">
          <h5>⭐ 口碑见证</h5>
          <p>自开业以来，我们累计服务<strong>5000+人次</strong>，用户好评率<strong>98%</strong>，是北京地区评分最高的剧本杀门店之一。</p>
          <ul>
            <li>🏆 2022年度"最受欢迎剧本杀门店"</li>
            <li>🏆 2023年度"最佳服务质量奖"</li>
            <li>🏆 大众点评五星商户</li>
            <li>🏆 美团必吃榜推荐商家</li>
          </ul>
          <p><em>"环境超棒，DM专业，剧本丰富，每次来都有新体验！"</em> - 来自会员@推理狂魔</p>
          <p><em>"朋友聚会的首选，氛围好服务好，强烈推荐！"</em> - 来自会员@剧本杀爱好者</p>
        </div>
        
        <div class="store-intro">
          <h5>🎉 特色活动</h5>
          <ul>
            <li>🎭 <strong>主题活动日：</strong>每月举办主题派对，剧本联动，惊喜不断</li>
            <li>🎓 <strong>新手专场：</strong>每周末开设新手专场，DM手把手教学</li>
            <li>💰 <strong>团购优惠：</strong>3人及以上享团购价，6人车本更优惠</li>
            <li>🎂 <strong>生日专属：</strong>生日当月游戏享8折优惠，还有神秘礼物</li>
            <li>🎁 <strong>会员福利：</strong>充值送优惠券，积分兑换剧本和周边</li>
          </ul>
        </div>
        
        <div class="store-intro store-contact">
          <h5>📞 联系我们</h5>
          <p>营业时间：10:00 - 22:00（全年无休）</p>
          <p>预约电话：010-12345678</p>
          <p>客服微信：探案密室官方客服</p>
          <p>官方微信公众号：探案密室</p>
          <p>门店地址：北京市朝阳区三里屯路xx号</p>
        </div>
        
        <div class="store-tips">
          <h5>💡 温馨提示</h5>
          <ul>
            <li>📅 建议提前1-3天预约，周末及节假日请尽早预约</li>
            <li>⏰ 请提前15分钟到店，以便DM讲解规则</li>
            <li>👥 部分剧本有人数要求，拼车可联系客服</li>
            <li>🎒 游戏过程中请遵守规则，爱护道具和设施</li>
            <li>📸 拍照留念请关闭闪光灯，避免影响他人体验</li>
          </ul>
        </div>
      `,environmentImages:["https://dummyimage.com/300x200/667eea/ffffff&text=大厅","https://dummyimage.com/300x200/f093fb/ffffff&text=推理房","https://dummyimage.com/300x200/4facfe/ffffff&text=沉浸房","https://dummyimage.com/300x200/43e97b/ffffff&text=休息区","https://dummyimage.com/300x200/fa709a/ffffff&text=恐怖房","https://dummyimage.com/300x200/fee140/333333&text=欢乐房"],coverImage:"",latitude:39.908815,longitude:116.397529}}finally{q.value=!1}},ie=async()=>{const s=await xt();s==="granted"?we():s==="denied"?(w.warning("位置权限已被拒绝，请在浏览器设置中手动开启"),b.value="请在浏览器设置中开启位置权限"):Z.value=!0},he=s=>{var e,r;if(b.value="",(e=n.value)!=null&&e.latitude&&((r=n.value)!=null&&r.longitude)){const h={latitude:n.value.latitude,longitude:n.value.longitude};F.value=fe(s,h)}},ye=()=>{b.value="位置权限被拒绝"},we=async()=>{if(n.value){Y.value=!0,b.value="";try{const s=await Dt(!0);if(n.value.latitude&&n.value.longitude){const e={latitude:n.value.latitude,longitude:n.value.longitude};F.value=fe(s,e),w.success("距离计算成功")}else w.warning("门店位置信息不完整")}catch(s){console.error("获取位置失败:",s),s.code===1?(b.value="位置权限被拒绝，点击重试",w.warning("请允许浏览器访问您的位置")):s.code===2?(b.value="无法获取位置信息",w.error("无法获取您的位置信息")):s.code===3?(b.value="获取位置超时",w.error("获取位置超时，请重试")):(b.value="浏览器不支持地理定位",w.error("您的浏览器不支持地理定位功能"))}finally{Y.value=!1}}},ke=async()=>{try{const s=await ht(T.params.id);s.data&&(R.value=s.data)}catch(s){console.error("加载房间信息失败:",s),R.value=[{id:1,name:"推理房A",type:1,capacity:4,description:"适合新手玩家，温馨舒适的小型推理房间",status:1},{id:2,name:"推理房B",type:2,capacity:6,description:"中型房间，配备专业道具和设施",status:1},{id:3,name:"沉浸房C",type:3,capacity:8,description:"大型沉浸式剧本房间，提供最佳游戏体验",status:0},{id:4,name:"欢乐房D",type:2,capacity:6,description:"适合欢乐向剧本，氛围轻松",status:1},{id:5,name:"机制房E",type:2,capacity:7,description:"配备多种机关道具，适合机制本",status:1},{id:6,name:"恐怖房F",type:1,capacity:5,description:"专为恐怖剧本设计，音效灯光俱全",status:0}]}},ae=async()=>{try{const s=await yt({storeId:T.params.id,page:1,pageSize:10});if(s.data){const e=s.data.records||s.data.list||[];V.value=e.map(r=>({id:r.id,username:r.isAnonymous===1?"匿名用户":r.userNickname||r.userName||r.username||"神秘玩家",userAvatar:r.isAnonymous===1?null:r.userAvatar||r.avatar||null,rating:Number(r.rating||r.overallRating||5),content:r.content||"该用户未填写评价内容",images:r.images||"",createTime:r.createTime||""}))}}catch(s){console.error("加载评价失败:",s)}},te=()=>{E.push({path:"/reservation/schedule",query:{storeId:n.value.id}})},xe=()=>{var s;(s=n.value)!=null&&s.phone?window.location.href=`tel:${n.value.phone}`:w.info("暂无联系电话")},De=(s,e)=>{s.data&&_.images.push(s.data)},be=s=>{var r;const e=((r=s.response)==null?void 0:r.data)||s.url;_.images=_.images.filter(h=>h!==e)},Ce=s=>{const e=s.type.startsWith("image/"),r=s.size/1024/1024<2;return e?r?!0:(w.error("图片大小不能超过2MB"),!1):(w.error("只能上传图片文件"),!1)},Me=async()=>{if(!X.isLoggedIn){w.warning("请先登录"),E.push("/login");return}if(!_.content.trim()){w.warning("请输入评价内容");return}try{await wt({storeId:T.params.id,rating:_.rating,content:_.content,images:_.images.join(",")}),w.success("评价成功"),A.value=!1,_.rating=5,_.content="",_.images=[],_.imageList=[],await Promise.all([ee(),ae()])}catch(s){console.error("提交评价失败:",s)}},Se=s=>({1:"小房",2:"中房",3:"大房"})[s]||"普通",Le=s=>({1:"小型房间（2-4人）",2:"中型房间（5-7人）",3:"大型房间（8人以上）"})[s]||"标准房间",Ie=s=>{E.push({path:"/reservation/schedule",query:{storeId:n.value.id}})},Te=s=>{const e=["success","primary","warning","danger","info"];return e[s%e.length]},Ve=s=>{const e={WiFi:"Wifi","Wi-Fi":"Wifi",无线网络:"Wifi",空调:"Wind",暖气:"Sunny",饮品:"CoffeeCup",零食:"Food",停车:"Van",包厢:"House",投影:"VideoCamera",音响:"Headset",桌游:"Grid",休息区:"Coffee",卫生间:"Location",充电:"Lightning"};for(const[r,h]of Object.entries(e))if(s.includes(r))return h;return"CircleCheck"},ze=s=>{const e=["#409eff","#67c23a","#e6a23c","#f56c6c","#909399","#36cfc9"];return e[s%e.length]},Re=s=>{w.info("点击查看大图")};Xe(()=>{if(X.isLoggedIn&&n.value&&J.value){const s=Math.floor((Date.now()-J.value)/1e3);s>0&&ve(2,n.value.id,s).catch(e=>{console.error("更新门店浏览时长失败:",e)})}});const $e=async()=>{if(T.params.id){j.value=!0;try{const s=await kt({storeId:T.params.id,days:7});(s.code===200||s.code===1)&&(z.value=Array.isArray(s.data)?s.data:[])}catch(s){console.error("加载门店场次失败:",s),z.value=[]}finally{j.value=!1}}},Ue=s=>{if(!s)return"";const e=new Date,r=new Date(e);r.setDate(e.getDate()+1);const h=new Date(s);return h.toDateString()===e.toDateString()?"今天":h.toDateString()===r.toDateString()?"明天":`${h.getMonth()+1}月${h.getDate()}日`},Ne=s=>s?String(s).substring(0,5):"",Ae=s=>{const e=s.maxPlayers-s.currentPlayers;return e<=0?"已满":e===1?"差1人成团":`余 ${e} 位`},Fe=s=>{const e=s.maxPlayers-s.currentPlayers;return e<=0?"remain-full":e<=2?"remain-few":"remain-ok"};return Ye(()=>{ee(),ke(),ae(),$e()}),(s,e)=>{const r=m("el-button"),h=m("el-empty"),se=m("el-carousel-item"),$=m("el-carousel"),x=m("el-col"),U=m("el-rate"),c=m("el-icon"),k=m("el-tag"),W=m("el-tooltip"),N=m("el-descriptions-item"),G=m("el-descriptions"),B=m("el-row"),H=m("el-card"),We=m("el-alert"),Be=m("el-avatar"),He=m("el-image"),le=m("el-form-item"),Pe=m("el-input"),Ee=m("el-upload"),qe=m("el-form"),je=m("el-dialog"),Oe=Je("loading");return Ze((a(),p("div",Mt,[!q.value&&!n.value?(a(),v(h,{key:0,description:"门店信息加载失败","image-size":200},{default:l(()=>[t(r,{type:"primary",onClick:ee},{default:l(()=>[...e[9]||(e[9]=[d(" 重新加载 ",-1)])]),_:1})]),_:1})):f("",!0),n.value?(a(),v(H,{key:1,class:"store-main-card"},{default:l(()=>[o("div",St,[t(B,{gutter:30},{default:l(()=>[t(x,{xs:24,md:12},{default:l(()=>[o("div",Lt,[t($,{height:"400px"},{default:l(()=>[(a(!0),p(L,null,I(me.value,(i,S)=>(a(),v(se,{key:S},{default:l(()=>[o("img",{src:i,alt:n.value.name},null,8,It)]),_:2},1024))),128))]),_:1})])]),_:1}),t(x,{xs:24,md:12},{default:l(()=>[o("div",Tt,[o("h1",null,u(n.value.name),1),o("div",Vt,[t(U,{modelValue:n.value.rating,"onUpdate:modelValue":e[0]||(e[0]=i=>n.value.rating=i),disabled:"","show-score":"",size:"large"},null,8,["modelValue"]),o("span",zt,"("+u(O.value)+"条评价)",1)]),t(G,{column:1,class:"store-desc"},{default:l(()=>[t(N,null,{label:l(()=>[t(c,null,{default:l(()=>[t(g(ue))]),_:1}),e[10]||(e[10]=d(" 地址 ",-1))]),default:l(()=>[o("div",Rt,[o("span",null,u(n.value.address),1),F.value?(a(),v(k,{key:0,type:"info",size:"small",class:"distance-tag"},{default:l(()=>[t(c,null,{default:l(()=>[t(g(st))]),_:1}),d(" 距离我 "+u(F.value),1)]),_:1})):f("",!0),!F.value&&!b.value?(a(),v(r,{key:1,type:"primary",text:"",size:"small",onClick:ie,loading:Y.value},{default:l(()=>[t(c,null,{default:l(()=>[t(g(ue))]),_:1}),e[11]||(e[11]=d(" 获取距离 ",-1))]),_:1},8,["loading"])):f("",!0),b.value?(a(),v(W,{key:2,content:b.value,placement:"top"},{default:l(()=>[t(k,{type:"warning",size:"small",onClick:ie,style:{cursor:"pointer"}},{default:l(()=>[t(c,null,{default:l(()=>[t(g(lt))]),_:1}),e[12]||(e[12]=d(" 点击重试 ",-1))]),_:1})]),_:1},8,["content"])):f("",!0)])]),_:1}),t(N,null,{label:l(()=>[t(c,null,{default:l(()=>[t(g(de))]),_:1}),e[13]||(e[13]=d(" 电话 ",-1))]),default:l(()=>[d(" "+u(n.value.phone),1)]),_:1}),t(N,null,{label:l(()=>[t(c,null,{default:l(()=>[t(g(oe))]),_:1}),e[14]||(e[14]=d(" 营业时间 ",-1))]),default:l(()=>[M.value?(a(),v(k,{key:0,type:M.value.open?"success":"danger",size:"small",style:{"margin-right":"8px"},effect:M.value.urgent||M.value.soon?"dark":"plain"},{default:l(()=>[d(u(M.value.text),1)]),_:1},8,["type","effect"])):f("",!0),d(" "+u(n.value.openTime)+" - "+u(n.value.closeTime),1)]),_:1})]),_:1}),o("div",$t,[(a(!0),p(L,null,I(n.value.tags,i=>(a(),v(k,{key:i,type:"success"},{default:l(()=>[d(u(i),1)]),_:2},1024))),128))]),o("div",Ut,[t(r,{class:"btn-reserve",size:"large",onClick:te},{default:l(()=>[t(c,null,{default:l(()=>[t(g(ce))]),_:1}),e[15]||(e[15]=d(" 立即预约 ",-1))]),_:1}),t(r,{class:"btn-call",size:"large",onClick:xe},{default:l(()=>[t(c,null,{default:l(()=>[t(g(de))]),_:1}),e[16]||(e[16]=d(" 电话咨询 ",-1))]),_:1})])])]),_:1})]),_:1})])]),_:1})):f("",!0),t(H,{class:"detail-card store-intro-card"},{header:l(()=>[o("div",Nt,[e[17]||(e[17]=o("span",null,"📖 门店介绍",-1)),t(k,{type:"warning",size:"small"},{default:l(()=>{var i;return[d(" ⭐ "+u(((i=n.value)==null?void 0:i.rating)||4.8)+" 分 ",1)]}),_:1})])]),default:l(()=>{var i,S,P,re;return[(i=n.value)!=null&&i.highlights&&n.value.highlights.length>0?(a(),p("div",At,[o("h4",Ft,[t(c,null,{default:l(()=>[t(g(ot))]),_:1}),e[18]||(e[18]=d(" 门店特色 ",-1))]),o("div",Wt,[(a(!0),p(L,null,I(n.value.highlights,(y,C)=>(a(),v(k,{key:C,type:Te(C),effect:"plain",size:"large",class:"highlight-tag"},{default:l(()=>[t(c,null,{default:l(()=>[t(g(nt))]),_:1}),d(" "+u(y),1)]),_:2},1032,["type"]))),128))])])):f("",!0),(S=n.value)!=null&&S.facilities&&n.value.facilities.length>0?(a(),p("div",Bt,[o("h4",Ht,[t(c,null,{default:l(()=>[t(g(it))]),_:1}),e[19]||(e[19]=d(" 门店设施 ",-1))]),t(B,{gutter:15,class:"facility-list"},{default:l(()=>[(a(!0),p(L,null,I(n.value.facilities,(y,C)=>(a(),v(x,{xs:12,sm:8,md:6,key:C},{default:l(()=>[o("div",Pt,[t(c,{color:ze(C)},{default:l(()=>[(a(),v(at(Ve(y))))]),_:2},1032,["color"]),o("span",null,u(y),1)])]),_:2},1024))),128))]),_:1})])):f("",!0),o("div",Et,[o("h4",qt,[t(c,null,{default:l(()=>[t(g(rt))]),_:1}),e[20]||(e[20]=d(" 门店简介 ",-1))]),o("div",{class:"store-description",innerHTML:((P=n.value)==null?void 0:P.description)||"这是一家优质的剧本杀门店，环境舒适，服务专业，拥有丰富的剧本资源和专业的DM团队。"},null,8,jt)]),o("div",Ot,[o("h4",Gt,[t(c,null,{default:l(()=>[t(g(oe))]),_:1}),e[21]||(e[21]=d(" 营业信息 ",-1))]),t(B,{gutter:20,class:"business-info-grid"},{default:l(()=>[t(x,{xs:24,sm:12,md:6},{default:l(()=>{var y,C;return[o("div",Kt,[o("div",Qt,[t(c,null,{default:l(()=>[t(g(oe))]),_:1})]),o("div",Xt,[e[22]||(e[22]=o("div",{class:"info-label"},"营业时间",-1)),o("div",Yt,[M.value?(a(),v(k,{key:0,type:M.value.open?"success":"danger",size:"small",style:{"margin-right":"8px"},effect:M.value.urgent||M.value.soon?"dark":"plain"},{default:l(()=>[d(u(M.value.text),1)]),_:1},8,["type","effect"])):f("",!0),d(" "+u(((y=n.value)==null?void 0:y.openTime)||"10:00")+" - "+u(((C=n.value)==null?void 0:C.closeTime)||"22:00"),1)])])])]}),_:1}),t(x,{xs:24,sm:12,md:6},{default:l(()=>{var y;return[o("div",Zt,[o("div",Jt,[t(c,null,{default:l(()=>[t(g(ge))]),_:1})]),o("div",es,[e[23]||(e[23]=o("div",{class:"info-label"},"已服务",-1)),o("div",ts,u(((y=n.value)==null?void 0:y.serviceCount)||"5000+")+" 人次",1)])])]}),_:1}),t(x,{xs:24,sm:12,md:6},{default:l(()=>{var y;return[o("div",ss,[o("div",ls,[t(c,null,{default:l(()=>[t(g(ut))]),_:1})]),o("div",os,[e[24]||(e[24]=o("div",{class:"info-label"},"剧本数量",-1)),o("div",ns,u(((y=n.value)==null?void 0:y.scriptCount)||"200+")+" 个",1)])])]}),_:1}),t(x,{xs:24,sm:12,md:6},{default:l(()=>[o("div",is,[o("div",as,[t(c,null,{default:l(()=>[t(g(pe))]),_:1})]),o("div",rs,[e[25]||(e[25]=o("div",{class:"info-label"},"房间数量",-1)),o("div",us,u(R.value.length)+" 间",1)])])]),_:1})]),_:1})]),(re=n.value)!=null&&re.environmentImages&&n.value.environmentImages.length>0?(a(),p("div",ds,[o("h4",cs,[t(c,null,{default:l(()=>[t(g(dt))]),_:1}),e[26]||(e[26]=d(" 门店环境 ",-1))]),t(B,{gutter:15,class:"environment-gallery"},{default:l(()=>[(a(!0),p(L,null,I(n.value.environmentImages,(y,C)=>(a(),v(x,{xs:12,sm:8,md:6,key:C},{default:l(()=>[o("div",{class:"environment-image",onClick:Hs=>Re(y)},[o("img",{src:y,alt:`门店环境 ${C+1}`},null,8,ps),o("div",vs,[t(c,null,{default:l(()=>[t(g(ct))]),_:1})])],8,gs)]),_:2},1024))),128))]),_:1})])):f("",!0)]}),_:1}),t(H,{class:"detail-card rooms-card"},{header:l(()=>[o("div",fs,[e[27]||(e[27]=o("span",null,"🚪 房间信息",-1)),ne.value>0?(a(),v(k,{key:0,type:"danger",size:"small"},{default:l(()=>[d(u(ne.value)+" 间可预约 ",1)]),_:1})):f("",!0)])]),default:l(()=>[t(B,{gutter:20},{default:l(()=>[(a(!0),p(L,null,I(R.value,i=>(a(),v(x,{xs:24,sm:12,md:8,key:i.id},{default:l(()=>[o("div",{class:Q(["room-card",{"room-unavailable":i.status!==1}])},[o("div",{class:Q(["room-type-badge",`room-type-${i.type}`])},u(Se(i.type)),3),o("div",{class:Q(["room-status-corner",{available:i.status===1}])},[i.status===1?(a(),v(c,{key:0},{default:l(()=>[t(g(gt))]),_:1})):(a(),v(c,{key:1},{default:l(()=>[t(g(pt))]),_:1}))],2),o("div",ms,[o("h4",null,u(i.name),1),t(k,{type:i.status===1?"success":"info",size:"small"},{default:l(()=>[d(u(i.status===1?"可预约":"已占用"),1)]),_:2},1032,["type"])]),o("div",_s,[o("div",hs,[t(c,null,{default:l(()=>[t(g(ge))]),_:1}),o("span",null,[e[28]||(e[28]=d("容纳人数：",-1)),o("strong",null,u(i.capacity),1),e[29]||(e[29]=d(" 人",-1))])]),i.type?(a(),p("div",ys,[t(c,null,{default:l(()=>[t(g(pe))]),_:1}),o("span",null,"房间类型："+u(Le(i.type)),1)])):f("",!0),i.description?(a(),p("div",ws,[t(c,null,{default:l(()=>[t(g(vt))]),_:1}),o("span",null,u(i.description),1)])):f("",!0)]),i.status===1?(a(),p("div",ks,[t(r,{type:"primary",size:"small",onClick:ft(S=>Ie(i),["stop"]),plain:""},{default:l(()=>[t(c,null,{default:l(()=>[t(g(ce))]),_:1}),e[30]||(e[30]=d(" 预约此房间 ",-1))]),_:1},8,["onClick"])])):f("",!0)],2)]),_:2},1024))),128))]),_:1}),R.value.length===0?(a(),v(h,{key:0,description:"暂无房间信息"},{default:l(()=>[t(r,{type:"primary",onClick:te},{default:l(()=>[...e[31]||(e[31]=[d("立即预约",-1)])]),_:1})]),_:1})):f("",!0)]),_:1}),t(H,{class:"detail-card schedule-card"},{header:l(()=>[o("div",xs,[e[32]||(e[32]=o("span",null,"🗓️ 近7天可约场次",-1)),j.value?f("",!0):(a(),v(k,{key:0,type:"success",size:"small"},{default:l(()=>[d(u(z.value.length>0?`${z.value.length} 个场次可约`:"暂无排期"),1)]),_:1}))])]),default:l(()=>[j.value?(a(),p("div",Ds,"加载中...")):z.value.length===0?(a(),p("div",bs," 🕰️ 暂无可约场次，可致电门店咨询 ")):(a(),p("div",Cs,[(a(!0),p(L,null,I(z.value,i=>(a(),p("div",{key:i.id,class:"store-schedule-item",onClick:e[1]||(e[1]=S=>g(E).push({path:"/reservation/schedule",query:{storeId:n.value.value.id}}))},[o("div",Ms,u(Ue(i.scheduleDate)),1),o("div",Ss,u(Ne(i.startTime)),1),i.scriptName?(a(),p("div",Ls,u(i.scriptName),1)):f("",!0),o("div",{class:Q(["ss-remain",Fe(i)])},u(Ae(i)),3)]))),128))])),z.value.length>0?(a(),p("div",Is,[t(r,{type:"primary",size:"small",onClick:te},{default:l(()=>[...e[33]||(e[33]=[d("立即预约",-1)])]),_:1})])):f("",!0)]),_:1}),t(H,{class:"detail-card reviews-card"},{header:l(()=>[o("div",Ts,[o("span",null,"💬 玩家评价 ("+u(O.value)+")",1),t(r,{type:"danger",size:"small",onClick:e[2]||(e[2]=i=>A.value=!0)},{default:l(()=>[...e[34]||(e[34]=[d(" ✍️ 写评价 ",-1)])]),_:1})])]),default:l(()=>[o("div",Vs,[V.value.length>0&&n.value&&O.value>V.value.length?(a(),p("div",zs,[t(We,{type:"info",closable:!1,"show-icon":!0},{default:l(()=>[d(" 当前显示最新的 "+u(V.value.length)+" 条评价，共 "+u(O.value)+" 条评价 ",1)]),_:1})])):f("",!0),(a(!0),p(L,null,I(V.value,i=>(a(),p("div",{class:"review-item",key:i.id},[o("div",Rs,[t(Be,{src:i.userAvatar,size:40},null,8,["src"]),o("div",$s,[o("div",Us,u(i.username),1),t(U,{modelValue:i.rating,"onUpdate:modelValue":S=>i.rating=S,disabled:"",size:"small"},null,8,["modelValue","onUpdate:modelValue"])]),o("div",Ns,u(i.createTime),1)]),o("div",As,u(i.content),1),i.images?(a(),p("div",Fs,[(a(!0),p(L,null,I(i.images.split(","),(S,P)=>(a(),v(He,{key:P,src:S,"preview-src-list":i.images.split(","),"initial-index":P,fit:"cover",style:{width:"80px",height:"80px","border-radius":"4px","margin-right":"8px","margin-top":"8px"}},null,8,["src","preview-src-list","initial-index"]))),128))])):f("",!0)]))),128)),V.value.length===0?(a(),v(h,{key:1,description:"暂无评价"})):f("",!0)])]),_:1}),t(je,{modelValue:A.value,"onUpdate:modelValue":e[7]||(e[7]=i=>A.value=i),title:"评价门店",width:"500px"},{footer:l(()=>[t(r,{onClick:e[6]||(e[6]=i=>A.value=!1)},{default:l(()=>[...e[36]||(e[36]=[d("取消",-1)])]),_:1}),t(r,{type:"primary",onClick:Me},{default:l(()=>[...e[37]||(e[37]=[d("提交",-1)])]),_:1})]),default:l(()=>[t(qe,{model:_,"label-width":"80px"},{default:l(()=>[t(le,{label:"评分"},{default:l(()=>[t(U,{modelValue:_.rating,"onUpdate:modelValue":e[3]||(e[3]=i=>_.rating=i),"show-text":""},null,8,["modelValue"])]),_:1}),t(le,{label:"评价内容"},{default:l(()=>[t(Pe,{modelValue:_.content,"onUpdate:modelValue":e[4]||(e[4]=i=>_.content=i),type:"textarea",rows:5,placeholder:"请输入评价内容"},null,8,["modelValue"])]),_:1}),t(le,{label:"打卡晒图"},{default:l(()=>[t(Ee,{"file-list":_.imageList,"onUpdate:fileList":e[5]||(e[5]=i=>_.imageList=i),action:"/api/upload/image","list-type":"picture-card",limit:6,accept:"image/*","on-success":De,"on-remove":be,"before-upload":Ce},{default:l(()=>[t(c,null,{default:l(()=>[t(g(mt))]),_:1})]),_:1},8,["file-list"]),e[35]||(e[35]=o("div",{class:"upload-tip"},"最多上传6张图片，每张不超过2MB",-1))]),_:1})]),_:1},8,["model"])]),_:1},8,["modelValue"]),t(Ct,{modelValue:Z.value,"onUpdate:modelValue":e[8]||(e[8]=i=>Z.value=i),onSuccess:he,onDeny:ye},null,8,["modelValue"])])),[[Oe,q.value]])}}},Ks=Ge(Ws,[["__scopeId","data-v-136e4e7f"]]);export{Ks as default};
