import{_ as Rt,u as Lt,r as x,a as St,I as O,l as Tt,C as Ft,D as Wt,c as f,m as v,n as _,d as t,w as e,f as g,i as $t,h as Ut,o as a,k as r,b as l,F as I,A as z,t as u,g as c,X as Nt,Y as tt,Z as Ht,$ as et,R as j,O as st,S as At,y as Bt,a0 as Et,H as qt,a1 as Ot,L as lt,a2 as jt,a3 as ot,J as Gt,a4 as Jt,N as Pt,v as G,a5 as Xt,a6 as Yt,a7 as Zt,e as Kt,a8 as Qt,E as h}from"./index-mtb8gjqy.js";import{b as te,c as ee,d as se,e as le}from"./store-B4hdm6vI.js";import{a as oe,g as it,b as nt}from"./store-covers-B9TnOBNv.js";const ie={class:"store-detail-container"},ne={class:"store-header"},ae={class:"store-images"},re=["src","alt"],ue={class:"store-basic-info"},de={class:"store-rating"},ce={class:"review-count"},ge={class:"address-with-distance"},ve={class:"store-tags"},pe={class:"action-buttons"},fe={class:"card-header"},_e={key:0,class:"store-highlights"},me={class:"section-title"},he={class:"highlight-tags"},ye={key:1,class:"store-facilities"},we={class:"section-title"},ke={class:"facility-item"},Ce={class:"store-description-section"},xe={class:"section-title"},be=["innerHTML"],De={class:"store-business-info"},Me={class:"section-title"},Ve={class:"info-card"},Ie={class:"info-icon"},ze={class:"info-content"},Re={class:"info-value"},Le={class:"info-card"},Se={class:"info-icon"},Te={class:"info-content"},Fe={class:"info-value"},We={class:"info-card"},$e={class:"info-icon"},Ue={class:"info-content"},Ne={class:"info-value"},He={class:"info-card"},Ae={class:"info-icon"},Be={class:"info-content"},Ee={class:"info-value"},qe={key:2,class:"store-environment"},Oe={class:"section-title"},je=["onClick"],Ge=["src","alt"],Je={class:"image-overlay"},Pe={class:"card-header"},Xe={class:"room-header"},Ye={class:"room-info"},Ze={class:"info-item"},Ke={key:0,class:"info-item"},Qe={key:1,class:"info-item"},ts={key:0,class:"room-action"},es={class:"card-header"},ss={class:"reviews-list"},ls={key:0,class:"review-tip"},os={class:"review-header"},is={class:"review-user"},ns={class:"username"},as={class:"review-date"},rs={class:"review-content"},us={__name:"detail",setup(ds){const L=$t(),S=Ut(),H=Lt(),$=x(!1),o=x(null),R=x([]),b=x([]),T=x(!1),F=x(""),A=x(!1),C=x(""),w=St({rating:5,content:""}),J=O(()=>R.value.filter(i=>i.status===1).length),U=O(()=>{var i;return(i=o.value)!=null&&i.reviewCount&&o.value.reviewCount>0?o.value.reviewCount:b.value.length}),at=O(()=>{var i,s;return(i=o.value)!=null&&i.coverImage?[o.value.coverImage]:(s=o.value)!=null&&s.name?[oe(o.value.name)]:["https://dummyimage.com/800x400/cccccc/666666&text=Store"]}),rt=(i="本店")=>`
    <div class="store-intro">
      <h5>🎭 关于我们</h5>
      <p><strong>${i}</strong>是一家专业的沉浸式剧本杀体验馆。我们致力于为每一位玩家打造难忘的推理之旅，用心呈现每一个故事，让您在虚拟与现实的交织中，体验不一样的人生。</p>
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
        <li>🔍 <strong>本格推理：</strong>《云使》《年轮》《魔术杀人事件》等经典本格</li>
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
  `,B=async()=>{$.value=!0;try{const i=await te(L.params.id);console.log("门店详情响应:",i),i.data&&(o.value=i.data,o.value.tags=o.value.tags||["环境优雅","服务专业","交通便利"],o.value.description||(o.value.description=rt(o.value.name)),o.value.latitude&&o.value.longitude&&P())}catch(i){console.error("加载门店详情失败:",i),o.value={id:L.params.id,name:"探案密室·沉浸式剧本体验馆",address:"北京市朝阳区三里屯路19号",phone:"010-12345678",openTime:"10:00",closeTime:"22:00",rating:4.8,reviewCount:128,serviceCount:"5000+",scriptCount:"200+",tags:["环境优雅","服务专业","交通便利"],highlights:["5年老店·口碑保证","专业DM团队·剧情还原度高","海量剧本·更新快","豪华装修·沉浸式体验","地铁直达·交通便利","免费饮品·舒适环境"],facilities:["WiFi","空调","饮品","零食","停车场","独立包厢","投影设备","音响系统","桌游区","休息区","卫生间","充电设施"],description:`
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
            <li>🔍 <strong>本格推理：</strong>《云使》《年轮》《魔术杀人事件》等经典本格</li>
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
      `,environmentImages:["https://dummyimage.com/300x200/667eea/ffffff&text=大厅","https://dummyimage.com/300x200/f093fb/ffffff&text=推理房","https://dummyimage.com/300x200/4facfe/ffffff&text=沉浸房","https://dummyimage.com/300x200/43e97b/ffffff&text=休息区","https://dummyimage.com/300x200/fa709a/ffffff&text=恐怖房","https://dummyimage.com/300x200/fee140/333333&text=欢乐房"],coverImage:"",latitude:39.908815,longitude:116.397529},P()}finally{$.value=!1}},ut=async()=>{if(o.value){A.value=!0,C.value="";try{const i=await it();if(o.value.latitude&&o.value.longitude){const s={latitude:o.value.latitude,longitude:o.value.longitude};F.value=nt(i,s),h.success("距离计算成功")}else h.warning("门店位置信息不完整")}catch(i){console.error("获取位置失败:",i),i.code===1?(C.value="位置权限被拒绝，请在浏览器设置中允许位置访问",h.warning("请允许浏览器访问您的位置")):i.code===2?(C.value="无法获取位置信息",h.error("无法获取您的位置信息")):i.code===3?(C.value="获取位置超时",h.error("获取位置超时，请重试")):(C.value="浏览器不支持地理定位",h.error("您的浏览器不支持地理定位功能"))}finally{A.value=!1}}},P=async()=>{var i,s;if(!(!((i=o.value)!=null&&i.latitude)||!((s=o.value)!=null&&s.longitude)))try{const m=await it(),D={latitude:o.value.latitude,longitude:o.value.longitude};F.value=nt(m,D)}catch{console.log("自动获取距离失败，用户可手动获取")}},dt=async()=>{try{const i=await ee(L.params.id);i.data&&(R.value=i.data)}catch(i){console.error("加载房间信息失败:",i),R.value=[{id:1,name:"推理房A",type:1,capacity:4,description:"适合新手玩家，温馨舒适的小型推理房间",status:1},{id:2,name:"推理房B",type:2,capacity:6,description:"中型房间，配备专业道具和设施",status:1},{id:3,name:"沉浸房C",type:3,capacity:8,description:"大型沉浸式剧本房间，提供最佳游戏体验",status:0},{id:4,name:"欢乐房D",type:2,capacity:6,description:"适合欢乐向剧本，氛围轻松",status:1},{id:5,name:"机制房E",type:2,capacity:7,description:"配备多种机关道具，适合机制本",status:1},{id:6,name:"恐怖房F",type:1,capacity:5,description:"专为恐怖剧本设计，音效灯光俱全",status:0}]}},X=async()=>{try{const i=await se({storeId:L.params.id,page:1,pageSize:10});i.data&&(b.value=i.data.records||i.data.list||[])}catch(i){console.error("加载评价失败:",i)}},Y=()=>{if(!H.isLoggedIn){h.warning("请先登录"),S.push("/login");return}S.push({path:"/reservation/create",query:{storeId:o.value.id}})},ct=()=>{var i;(i=o.value)!=null&&i.phone?window.location.href=`tel:${o.value.phone}`:h.info("暂无联系电话")},gt=async()=>{if(!H.isLoggedIn){h.warning("请先登录"),S.push("/login");return}if(!w.content.trim()){h.warning("请输入评价内容");return}try{await le({storeId:L.params.id,rating:w.rating,content:w.content}),h.success("评价成功"),T.value=!1,w.rating=5,w.content="",await Promise.all([B(),X()])}catch(i){console.error("提交评价失败:",i)}},vt=i=>({1:"小房",2:"中房",3:"大房"})[i]||"普通",pt=i=>({1:"小型房间（2-4人）",2:"中型房间（5-7人）",3:"大型房间（8人以上）"})[i]||"标准房间",ft=i=>{if(!H.isLoggedIn){h.warning("请先登录"),S.push("/login");return}S.push({path:"/reservation/create",query:{storeId:o.value.id,roomId:i.id,roomName:i.name}})},_t=i=>{const s=["success","primary","warning","danger","info"];return s[i%s.length]},mt=i=>{const s={WiFi:"Wifi","Wi-Fi":"Wifi",无线网络:"Wifi",空调:"Wind",暖气:"Sunny",饮品:"CoffeeCup",零食:"Food",停车:"Van",包厢:"House",投影:"VideoCamera",音响:"Headset",桌游:"Grid",休息区:"Coffee",卫生间:"Location",充电:"Lightning"};for(const[m,D]of Object.entries(s))if(i.includes(m))return D;return"CircleCheck"},ht=i=>{const s=["#409eff","#67c23a","#e6a23c","#f56c6c","#909399","#36cfc9"];return s[i%s.length]},yt=i=>{h.info("点击查看大图")};return Tt(()=>{B(),dt(),X()}),(i,s)=>{const m=g("el-button"),D=g("el-empty"),wt=g("el-carousel-item"),kt=g("el-carousel"),k=g("el-col"),E=g("el-rate"),d=g("el-icon"),M=g("el-tag"),Ct=g("el-tooltip"),q=g("el-descriptions-item"),xt=g("el-descriptions"),W=g("el-row"),N=g("el-card"),bt=g("el-alert"),Dt=g("el-avatar"),Z=g("el-form-item"),Mt=g("el-input"),Vt=g("el-form"),It=g("el-dialog"),zt=Wt("loading");return Ft((a(),f("div",ie,[!$.value&&!o.value?(a(),v(D,{key:0,description:"门店信息加载失败","image-size":200},{default:e(()=>[t(m,{type:"primary",onClick:B},{default:e(()=>[...s[6]||(s[6]=[r(" 重新加载 ",-1)])]),_:1})]),_:1})):_("",!0),o.value?(a(),v(N,{key:1},{default:e(()=>[l("div",ne,[t(W,{gutter:30},{default:e(()=>[t(k,{xs:24,md:12},{default:e(()=>[l("div",ae,[t(kt,{height:"400px"},{default:e(()=>[(a(!0),f(I,null,z(at.value,(n,V)=>(a(),v(wt,{key:V},{default:e(()=>[l("img",{src:n,alt:o.value.name},null,8,re)]),_:2},1024))),128))]),_:1})])]),_:1}),t(k,{xs:24,md:12},{default:e(()=>[l("div",ue,[l("h1",null,u(o.value.name),1),l("div",de,[t(E,{modelValue:o.value.rating,"onUpdate:modelValue":s[0]||(s[0]=n=>o.value.rating=n),disabled:"","show-score":"",size:"large"},null,8,["modelValue"]),l("span",ce,"("+u(U.value)+"条评价)",1)]),t(xt,{column:1,class:"store-desc"},{default:e(()=>[t(q,null,{label:e(()=>[t(d,null,{default:e(()=>[t(c(tt))]),_:1}),s[7]||(s[7]=r(" 地址 ",-1))]),default:e(()=>[l("div",ge,[l("span",null,u(o.value.address),1),F.value?(a(),v(M,{key:0,type:"info",size:"small",class:"distance-tag"},{default:e(()=>[t(d,null,{default:e(()=>[t(c(Nt))]),_:1}),r(" 距离我 "+u(F.value),1)]),_:1})):_("",!0),!F.value&&!C.value?(a(),v(m,{key:1,type:"primary",text:"",size:"small",onClick:ut,loading:A.value},{default:e(()=>[t(d,null,{default:e(()=>[t(c(tt))]),_:1}),s[8]||(s[8]=r(" 获取距离 ",-1))]),_:1},8,["loading"])):_("",!0),C.value?(a(),v(Ct,{key:2,content:C.value,placement:"top"},{default:e(()=>[t(M,{type:"warning",size:"small"},{default:e(()=>[t(d,null,{default:e(()=>[t(c(Ht))]),_:1}),s[9]||(s[9]=r(" 无法获取位置 ",-1))]),_:1})]),_:1},8,["content"])):_("",!0)])]),_:1}),t(q,null,{label:e(()=>[t(d,null,{default:e(()=>[t(c(et))]),_:1}),s[10]||(s[10]=r(" 电话 ",-1))]),default:e(()=>[r(" "+u(o.value.phone),1)]),_:1}),t(q,null,{label:e(()=>[t(d,null,{default:e(()=>[t(c(j))]),_:1}),s[11]||(s[11]=r(" 营业时间 ",-1))]),default:e(()=>[r(" "+u(o.value.openTime)+" - "+u(o.value.closeTime),1)]),_:1})]),_:1}),l("div",ve,[(a(!0),f(I,null,z(o.value.tags,n=>(a(),v(M,{key:n,type:"success"},{default:e(()=>[r(u(n),1)]),_:2},1024))),128))]),l("div",pe,[t(m,{type:"primary",size:"large",onClick:Y},{default:e(()=>[t(d,null,{default:e(()=>[t(c(st))]),_:1}),s[12]||(s[12]=r(" 立即预约 ",-1))]),_:1}),t(m,{size:"large",onClick:ct},{default:e(()=>[t(d,null,{default:e(()=>[t(c(et))]),_:1}),s[13]||(s[13]=r(" 电话咨询 ",-1))]),_:1})])])]),_:1})]),_:1})])]),_:1})):_("",!0),t(N,{class:"detail-card store-intro-card"},{header:e(()=>[l("div",fe,[s[14]||(s[14]=l("span",null,"门店介绍",-1)),t(M,{type:"primary",size:"small"},{default:e(()=>{var n;return[t(d,null,{default:e(()=>[t(c(Pt))]),_:1}),r(" "+u(((n=o.value)==null?void 0:n.rating)||4.8)+" 分 ",1)]}),_:1})])]),default:e(()=>{var n,V,K,Q;return[(n=o.value)!=null&&n.highlights&&o.value.highlights.length>0?(a(),f("div",_e,[l("h4",me,[t(d,null,{default:e(()=>[t(c(At))]),_:1}),s[15]||(s[15]=r(" 门店特色 ",-1))]),l("div",he,[(a(!0),f(I,null,z(o.value.highlights,(p,y)=>(a(),v(M,{key:y,type:_t(y),effect:"plain",size:"large",class:"highlight-tag"},{default:e(()=>[t(d,null,{default:e(()=>[t(c(Bt))]),_:1}),r(" "+u(p),1)]),_:2},1032,["type"]))),128))])])):_("",!0),(V=o.value)!=null&&V.facilities&&o.value.facilities.length>0?(a(),f("div",ye,[l("h4",we,[t(d,null,{default:e(()=>[t(c(Et))]),_:1}),s[16]||(s[16]=r(" 门店设施 ",-1))]),t(W,{gutter:15,class:"facility-list"},{default:e(()=>[(a(!0),f(I,null,z(o.value.facilities,(p,y)=>(a(),v(k,{xs:12,sm:8,md:6,key:y},{default:e(()=>[l("div",ke,[t(d,{color:ht(y)},{default:e(()=>[(a(),v(qt(mt(p))))]),_:2},1032,["color"]),l("span",null,u(p),1)])]),_:2},1024))),128))]),_:1})])):_("",!0),l("div",Ce,[l("h4",xe,[t(d,null,{default:e(()=>[t(c(Ot))]),_:1}),s[17]||(s[17]=r(" 门店简介 ",-1))]),l("div",{class:"store-description",innerHTML:((K=o.value)==null?void 0:K.description)||"这是一家优质的剧本杀门店，环境舒适，服务专业，拥有丰富的剧本资源和专业的DM团队。"},null,8,be)]),l("div",De,[l("h4",Me,[t(d,null,{default:e(()=>[t(c(j))]),_:1}),s[18]||(s[18]=r(" 营业信息 ",-1))]),t(W,{gutter:20,class:"business-info-grid"},{default:e(()=>[t(k,{xs:24,sm:12,md:6},{default:e(()=>{var p,y;return[l("div",Ve,[l("div",Ie,[t(d,null,{default:e(()=>[t(c(j))]),_:1})]),l("div",ze,[s[19]||(s[19]=l("div",{class:"info-label"},"营业时间",-1)),l("div",Re,u(((p=o.value)==null?void 0:p.openTime)||"10:00")+" - "+u(((y=o.value)==null?void 0:y.closeTime)||"22:00"),1)])])]}),_:1}),t(k,{xs:24,sm:12,md:6},{default:e(()=>{var p;return[l("div",Le,[l("div",Se,[t(d,null,{default:e(()=>[t(c(lt))]),_:1})]),l("div",Te,[s[20]||(s[20]=l("div",{class:"info-label"},"已服务",-1)),l("div",Fe,u(((p=o.value)==null?void 0:p.serviceCount)||"5000+")+" 人次",1)])])]}),_:1}),t(k,{xs:24,sm:12,md:6},{default:e(()=>{var p;return[l("div",We,[l("div",$e,[t(d,null,{default:e(()=>[t(c(jt))]),_:1})]),l("div",Ue,[s[21]||(s[21]=l("div",{class:"info-label"},"剧本数量",-1)),l("div",Ne,u(((p=o.value)==null?void 0:p.scriptCount)||"200+")+" 个",1)])])]}),_:1}),t(k,{xs:24,sm:12,md:6},{default:e(()=>[l("div",He,[l("div",Ae,[t(d,null,{default:e(()=>[t(c(ot))]),_:1})]),l("div",Be,[s[22]||(s[22]=l("div",{class:"info-label"},"房间数量",-1)),l("div",Ee,u(R.value.length)+" 间",1)])])]),_:1})]),_:1})]),(Q=o.value)!=null&&Q.environmentImages&&o.value.environmentImages.length>0?(a(),f("div",qe,[l("h4",Oe,[t(d,null,{default:e(()=>[t(c(Gt))]),_:1}),s[23]||(s[23]=r(" 门店环境 ",-1))]),t(W,{gutter:15,class:"environment-gallery"},{default:e(()=>[(a(!0),f(I,null,z(o.value.environmentImages,(p,y)=>(a(),v(k,{xs:12,sm:8,md:6,key:y},{default:e(()=>[l("div",{class:"environment-image",onClick:cs=>yt(p)},[l("img",{src:p,alt:`门店环境 ${y+1}`},null,8,Ge),l("div",Je,[t(d,null,{default:e(()=>[t(c(Jt))]),_:1})])],8,je)]),_:2},1024))),128))]),_:1})])):_("",!0)]}),_:1}),t(N,{class:"detail-card"},{header:e(()=>[l("div",Pe,[s[24]||(s[24]=l("span",null,"房间信息",-1)),J.value>0?(a(),v(M,{key:0,type:"success",size:"small"},{default:e(()=>[t(d,null,{default:e(()=>[t(c(Qt))]),_:1}),r(" "+u(J.value)+" 间可用 ",1)]),_:1})):_("",!0)])]),default:e(()=>[t(W,{gutter:20},{default:e(()=>[(a(!0),f(I,null,z(R.value,n=>(a(),v(k,{xs:24,sm:12,md:8,key:n.id},{default:e(()=>[l("div",{class:G(["room-card",{"room-unavailable":n.status!==1}])},[l("div",{class:G(["room-type-badge",`room-type-${n.type}`])},u(vt(n.type)),3),l("div",{class:G(["room-status-corner",{available:n.status===1}])},[n.status===1?(a(),v(d,{key:0},{default:e(()=>[t(c(Xt))]),_:1})):(a(),v(d,{key:1},{default:e(()=>[t(c(Yt))]),_:1}))],2),l("div",Xe,[l("h4",null,u(n.name),1),t(M,{type:n.status===1?"success":"info",size:"small"},{default:e(()=>[r(u(n.status===1?"可预约":"已占用"),1)]),_:2},1032,["type"])]),l("div",Ye,[l("div",Ze,[t(d,null,{default:e(()=>[t(c(lt))]),_:1}),l("span",null,[s[25]||(s[25]=r("容纳人数：",-1)),l("strong",null,u(n.capacity),1),s[26]||(s[26]=r(" 人",-1))])]),n.type?(a(),f("div",Ke,[t(d,null,{default:e(()=>[t(c(ot))]),_:1}),l("span",null,"房间类型："+u(pt(n.type)),1)])):_("",!0),n.description?(a(),f("div",Qe,[t(d,null,{default:e(()=>[t(c(Zt))]),_:1}),l("span",null,u(n.description),1)])):_("",!0)]),n.status===1?(a(),f("div",ts,[t(m,{type:"primary",size:"small",onClick:Kt(V=>ft(n),["stop"]),plain:""},{default:e(()=>[t(d,null,{default:e(()=>[t(c(st))]),_:1}),s[27]||(s[27]=r(" 预约此房间 ",-1))]),_:1},8,["onClick"])])):_("",!0)],2)]),_:2},1024))),128))]),_:1}),R.value.length===0?(a(),v(D,{key:0,description:"暂无房间信息"},{default:e(()=>[t(m,{type:"primary",onClick:Y},{default:e(()=>[...s[28]||(s[28]=[r("立即预约",-1)])]),_:1})]),_:1})):_("",!0)]),_:1}),t(N,{class:"detail-card"},{header:e(()=>[l("div",es,[l("span",null,"用户评价 ("+u(U.value)+"条)",1),t(m,{type:"primary",size:"small",onClick:s[1]||(s[1]=n=>T.value=!0)},{default:e(()=>[...s[29]||(s[29]=[r(" 写评价 ",-1)])]),_:1})])]),default:e(()=>[l("div",ss,[b.value.length>0&&o.value&&U.value>b.value.length?(a(),f("div",ls,[t(bt,{type:"info",closable:!1,"show-icon":!0},{default:e(()=>[r(" 当前显示最新的 "+u(b.value.length)+" 条评价，共 "+u(U.value)+" 条评价 ",1)]),_:1})])):_("",!0),(a(!0),f(I,null,z(b.value,n=>(a(),f("div",{class:"review-item",key:n.id},[l("div",os,[t(Dt,{src:n.userAvatar,size:40},null,8,["src"]),l("div",is,[l("div",ns,u(n.username),1),t(E,{modelValue:n.rating,"onUpdate:modelValue":V=>n.rating=V,disabled:"",size:"small"},null,8,["modelValue","onUpdate:modelValue"])]),l("div",as,u(n.createTime),1)]),l("div",rs,u(n.content),1)]))),128)),b.value.length===0?(a(),v(D,{key:1,description:"暂无评价"})):_("",!0)])]),_:1}),t(It,{modelValue:T.value,"onUpdate:modelValue":s[5]||(s[5]=n=>T.value=n),title:"评价门店",width:"500px"},{footer:e(()=>[t(m,{onClick:s[4]||(s[4]=n=>T.value=!1)},{default:e(()=>[...s[30]||(s[30]=[r("取消",-1)])]),_:1}),t(m,{type:"primary",onClick:gt},{default:e(()=>[...s[31]||(s[31]=[r("提交",-1)])]),_:1})]),default:e(()=>[t(Vt,{model:w,"label-width":"80px"},{default:e(()=>[t(Z,{label:"评分"},{default:e(()=>[t(E,{modelValue:w.rating,"onUpdate:modelValue":s[2]||(s[2]=n=>w.rating=n),"show-text":""},null,8,["modelValue"])]),_:1}),t(Z,{label:"评价内容"},{default:e(()=>[t(Mt,{modelValue:w.content,"onUpdate:modelValue":s[3]||(s[3]=n=>w.content=n),type:"textarea",rows:5,placeholder:"请输入评价内容"},null,8,["modelValue"])]),_:1})]),_:1},8,["model"])]),_:1},8,["modelValue"])])),[[zt,$.value]])}}},fs=Rt(us,[["__scopeId","data-v-696ec6f7"]]);export{fs as default};
