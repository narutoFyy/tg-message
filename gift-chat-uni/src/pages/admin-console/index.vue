<template>
  <view class="page-shell soft-page admin-page">
    <view v-if="isAdminReady" class="page-stack">
      <view class="admin-top-nav">
        <button class="nav-button active" @click="activeTab = 'users'">管理员总控台</button>
        <button class="nav-button" @click="goSupportChat">客服聊天</button>
        <button class="nav-button" @click="goRateAdmin">汇率管理</button>
        <button class="nav-button" @click="goUserHome">用户端首页</button>
      </view>

      <view class="panel">
        <text class="eyebrow">管理员后台</text>
        <view style="height: 12rpx"></view>
        <text class="title">平台运营总控台</text>
        <view style="height: 10rpx"></view>
        <text class="subtitle">统一查看用户、客服、订单、聊天、抽奖、风控和运营通知。</text>
      </view>

      <view class="admin-home-grid">
        <button class="quick-link" @click="activeTab = 'support'">
          <text class="quick-title">客服记录</text>
          <text class="quick-meta">{{ conversations.length }} 个会话</text>
        </button>
        <button class="quick-link" @click="goSupportChat">
          <text class="quick-title">实时客服聊天</text>
          <text class="quick-meta">以管理员身份查看和回复</text>
        </button>
        <button class="quick-link" @click="activeTab = 'orders'">
          <text class="quick-title">全部订单</text>
          <text class="quick-meta">{{ transactions.length }} 个订单</text>
        </button>
        <button class="quick-link" @click="activeTab = 'direct'">
          <text class="quick-title">私聊记录</text>
          <text class="quick-meta">{{ directConversations.length }} 条记录</text>
        </button>
      </view>

      <view class="panel tab-row">
        <button :class="['ghost-button', activeTab === 'users' && 'active-tab']" @click="activeTab = 'users'">用户管理</button>
        <button :class="['ghost-button', activeTab === 'growth' && 'active-tab']" @click="activeTab = 'growth'">增长抽奖</button>
        <button :class="['ghost-button', activeTab === 'agents' && 'active-tab']" @click="activeTab = 'agents'">客服账号</button>
        <button :class="['ghost-button', activeTab === 'support' && 'active-tab']" @click="activeTab = 'support'">客服记录</button>
        <button :class="['ghost-button', activeTab === 'direct' && 'active-tab']" @click="activeTab = 'direct'">私聊记录</button>
        <button :class="['ghost-button', activeTab === 'broadcast' && 'active-tab']" @click="activeTab = 'broadcast'">群发通知</button>
        <button :class="['ghost-button', activeTab === 'orders' && 'active-tab']" @click="activeTab = 'orders'">订单管理</button>
        <button :class="['ghost-button', activeTab === 'withdrawals' && 'active-tab']" @click="activeTab = 'withdrawals'">提现管理</button>
        <button :class="['ghost-button', activeTab === 'risks' && 'active-tab']" @click="activeTab = 'risks'">风控安全</button>
        <button :class="['ghost-button', activeTab === 'loans' && 'active-tab']" @click="activeTab = 'loans'">贷款申请</button>
        <button :class="['ghost-button', activeTab === 'rewards' && 'active-tab']" @click="activeTab = 'rewards'">奖励配置</button>
        <button :class="['ghost-button', activeTab === 'notifications' && 'active-tab']" @click="activeTab = 'notifications'">系统通知</button>
      </view>

      <view class="panel admin-balance-panel">
        <view>
          <text class="section-title">平台资金概览</text>
          <text class="row-meta">活跃用户：{{ store.state.balanceSummary?.userCount || 0 }}</text>
        </view>
        <view class="balance-grid">
          <view>
            <text class="row-meta">可用余额</text>
            <text class="balance-number">{{ store.state.balanceSummary?.availableTotal || '0.00' }}</text>
          </view>
          <view>
            <text class="row-meta">待结算</text>
            <text class="balance-number">{{ store.state.balanceSummary?.pendingTotal || '0.00' }}</text>
          </view>
          <view>
            <text class="row-meta">提现处理中</text>
            <text class="balance-number">{{ store.state.balanceSummary?.pendingWithdrawalTotal || '0.00' }}</text>
          </view>
          <view>
            <text class="row-meta">已提现</text>
            <text class="balance-number">{{ store.state.balanceSummary?.withdrawnTotal || '0.00' }}</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'users'" class="panel">
        <text class="section-title">用户管理</text>
        <view style="height: 18rpx"></view>
        <view v-for="user in users" :key="user.id" class="list-row">
          <view>
            <text class="row-title">{{ user.username }}</text>
            <text class="row-meta">{{ user.phone || '无手机号' }} / {{ user.email || '无邮箱' }}</text>
            <text class="row-meta">{{ user.role }} / {{ user.status }} / {{ user.vipLevel }} / USD {{ user.vipPoints }} completed / {{ user.createdAt }}</text>
          </view>
          <text :class="['status-pill', user.blacklisted ? 'paused' : 'active']">
            {{ user.blacklisted ? '已拉黑' : '正常' }}
          </text>
        </view>
      </view>

      <view v-if="activeTab === 'agents'" class="panel">
        <text class="section-title">创建客服账号</text>
        <view style="height: 18rpx"></view>
        <input v-model="agentForm.username" class="field-input" placeholder="客服用户名" />
        <view style="height: 14rpx"></view>
        <input v-model="agentForm.email" class="field-input" placeholder="邮箱" />
        <view style="height: 14rpx"></view>
        <input v-model="agentForm.phone" class="field-input" placeholder="手机号" />
        <view style="height: 14rpx"></view>
        <input v-model="agentForm.password" class="field-input" password placeholder="密码" />
        <view style="height: 18rpx"></view>
        <button class="primary-button" @click="submitAgent">创建客服</button>
      </view>

      <view v-if="activeTab === 'agents'" class="panel">
        <text class="section-title">客服账号</text>
        <view style="height: 18rpx"></view>
        <view v-for="agent in agents" :key="agent.id" class="list-row agent-list-row">
          <view>
            <text class="row-title">{{ agent.username }}</text>
            <text class="row-meta">负责 {{ agent.assignedConversationCount }} 个会话</text>
            <text class="row-meta">{{ agent.phone || '无手机号' }} / {{ agent.email || '无邮箱' }}</text>
            <text class="row-meta">欢迎语：{{ agent.welcomeMessageEnabled ? '已启用' : '未启用' }}{{ agent.welcomeMessageUpdatedAt ? ` / ${agent.welcomeMessageUpdatedAt}` : '' }}</text>
          </view>
          <view class="agent-row-actions">
            <button class="ghost-button mini-button" @click="editWelcomeMessage(agent)">
              {{ editingWelcomeAgentId === agent.id ? '收起' : '编辑欢迎语' }}
            </button>
            <button class="ghost-button mini-button" @click="toggleAgent(agent.id, agent.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')">
              {{ agent.status === 'ACTIVE' ? '停用' : '启用' }}
            </button>
          </view>
          <view v-if="editingWelcomeAgentId === agent.id" class="welcome-editor">
            <view class="reward-setting-row">
              <view>
                <text class="row-title">注册自动欢迎语</text>
                <text class="row-meta">新用户分配到 {{ agent.username }} 后自动收到这条消息。</text>
              </view>
              <switch
                :checked="welcomeDrafts[agent.id]?.enabled"
                @change="setWelcomeEnabled(agent.id, $event)"
              />
            </view>
            <textarea
              v-model="welcomeDrafts[agent.id].content"
              class="field-input welcome-textarea"
              maxlength="1000"
              placeholder="请输入客服欢迎语"
            />
            <view class="welcome-editor-footer">
              <text class="row-meta">{{ welcomeDrafts[agent.id]?.content.length || 0 }}/1000</text>
              <button class="primary-button mini-button" @click="saveWelcomeMessage(agent)">保存欢迎语</button>
            </view>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'support'" class="panel">
        <text class="section-title">客服会话记录</text>
        <view style="height: 18rpx"></view>
        <view v-for="conversation in conversations" :key="conversation.conversationId" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ conversation.customerUsername }}</text>
              <text class="row-meta">{{ conversation.assignmentStatus }} / {{ conversation.assignedAgent || '未分配' }}</text>
            </view>
            <view class="support-row-actions">
              <text class="status-pill active">{{ conversation.messages.length }} 条消息</text>
              <button class="ghost-button mini-button" @click="goSupportConversation(conversation.conversationId)">查看聊天</button>
            </view>
          </view>
          <view class="assign-row">
            <input v-model="assignDrafts[conversation.conversationId]" class="field-input assign-input" placeholder="客服用户名" />
            <button class="primary-button mini-button" @click="assignConversation(conversation.conversationId)">分配客服</button>
          </view>
          <view v-if="conversation.messages[0]" class="last-message">
            {{ conversation.messages[conversation.messages.length - 1].content }}
          </view>
          <view v-if="conversation.messages.length" class="admin-message-list">
            <view
              v-for="message in conversation.messages"
              :key="message.id"
              class="admin-message"
            >
              <text class="message-meta">{{ message.author }} · {{ message.createdAt }} · {{ message.type }}</text>
              <text class="message-body">{{ message.content }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'direct'" class="panel">
        <text class="section-title">私聊记录</text>
        <view style="height: 18rpx"></view>
        <view class="assign-row">
          <input v-model="directSearch" class="field-input assign-input" placeholder="按用户名筛选" />
          <button class="primary-button mini-button" @click="refreshDirectConversations">搜索</button>
        </view>
        <view style="height: 18rpx"></view>
        <view v-for="conversation in directConversations" :key="conversation.friendshipId" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ conversation.requesterUsername }} / {{ conversation.addresseeUsername }}</text>
              <text class="row-meta">{{ conversation.status }} / {{ conversation.messages.length }} messages</text>
            </view>
          </view>
          <view v-if="conversation.messages.length" class="admin-message-list">
            <view
              v-for="message in conversation.messages"
              :key="message.id"
              class="admin-message"
            >
              <text class="message-meta">{{ message.author }} · {{ message.createdAt }} · {{ message.type }}</text>
              <text class="message-body">{{ message.content }}</text>
            </view>
          </view>
          <text v-else class="row-meta">No direct messages yet.</text>
        </view>
      </view>

      <view v-if="activeTab === 'broadcast'" class="panel">
        <text class="section-title">Admin broadcast</text>
        <view style="height: 18rpx"></view>
        <view class="broadcast-compose">
          <textarea
            v-model="broadcastForm.content"
            class="field-input broadcast-caption-input"
            :placeholder="broadcastForm.mediaUrl ? 'Optional caption for this media...' : 'Message all users...'"
          />
          <view class="broadcast-media-actions">
            <button class="ghost-button mini-button" :disabled="broadcastForm.uploading" @click="chooseAdminBroadcastImage">Choose image</button>
            <button class="ghost-button mini-button" :disabled="broadcastForm.uploading" @click="chooseAdminBroadcastVideo">Choose video</button>
            <button v-if="broadcastForm.mediaUrl" class="ghost-button mini-button danger-soft" @click="clearAdminBroadcastMedia()">Remove media</button>
            <button class="primary-button mini-button" :disabled="broadcastForm.uploading" @click="submitBroadcast">
              {{ broadcastForm.uploading ? 'Uploading...' : 'Send' }}
            </button>
          </view>
          <view v-if="broadcastForm.mediaUrl" class="broadcast-media-preview">
            <image
              v-if="broadcastForm.messageType === 'image'"
              class="broadcast-preview-image"
              :src="resolveMediaUrl(broadcastForm.mediaUrl)"
              mode="aspectFit"
            />
            <video
              v-else-if="broadcastForm.messageType === 'video'"
              class="broadcast-preview-video"
              :src="resolveMediaUrl(broadcastForm.mediaUrl)"
              controls
            />
            <text class="row-meta">{{ broadcastForm.mediaName || broadcastForm.messageType }}</text>
          </view>
        </view>
        <view style="height: 14rpx"></view>
        <view class="broadcast-type-row">
          <button
            v-for="type in broadcastTypes"
            :key="type"
            :class="['ghost-button', 'mini-button', broadcastForm.messageType === type && 'active-soft']"
            @click="setAdminBroadcastType(type)"
          >
            {{ type }}
          </button>
        </view>
        <view style="height: 14rpx"></view>
        <input v-model="broadcastForm.keyword" class="field-input assign-input" placeholder="Filter by note, username, phone, or bank account" />
        <view class="broadcast-type-row">
          <button
            v-for="code in adminBroadcastCountryOptions"
            :key="code"
            :class="['ghost-button', 'mini-button', broadcastForm.countryCodes.includes(code) && 'active-soft']"
            @click="toggleAdminBroadcastCountry(code)"
          >
            {{ code }}
          </button>
        </view>
      </view>

      <view v-if="activeTab === 'growth'" class="panel growth-summary-panel">
        <view class="growth-stat">
          <text class="row-meta">VIP0</text>
          <text class="balance-number">{{ vipCount('VIP0') }}</text>
        </view>
        <view class="growth-stat">
          <text class="row-meta">VIP1</text>
          <text class="balance-number">{{ vipCount('VIP1') }}</text>
        </view>
        <view class="growth-stat">
          <text class="row-meta">VIP2</text>
          <text class="balance-number">{{ vipCount('VIP2') }}</text>
        </view>
        <view class="growth-stat">
          <text class="row-meta">VIP3</text>
          <text class="balance-number">{{ vipCount('VIP3') }}</text>
        </view>
        <view class="growth-stat">
          <text class="row-meta">VIP4</text>
          <text class="balance-number">{{ vipCount('VIP4') }}</text>
        </view>
        <view class="growth-stat">
          <text class="row-meta">VIP5</text>
          <text class="balance-number">{{ vipCount('VIP5') }}</text>
        </view>
      </view>

      <view v-if="activeTab === 'growth'" class="panel">
        <text class="section-title">VIP rules</text>
        <view style="height: 18rpx"></view>
        <view class="rule-grid">
          <view v-for="rule in vipRules" :key="rule.level" class="rule-card">
            <text class="row-title">{{ rule.level }}</text>
            <text class="row-meta">{{ rule.threshold }}</text>
            <text class="row-meta">{{ rule.draw }}</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'growth'" class="panel">
        <text class="section-title">User VIP progress</text>
        <view style="height: 18rpx"></view>
        <view v-for="user in userVipRows" :key="user.id" class="list-row">
          <view>
            <text class="row-title">{{ user.username }}</text>
            <text class="row-meta">{{ user.phone || 'No phone' }} / {{ user.email || 'No email' }}</text>
            <text class="row-meta">{{ user.vipLevel }} / USD {{ user.vipPoints }} completed / {{ user.status }}</text>
          </view>
          <view class="growth-user-actions">
            <input v-model="birthdayDrafts[user.id]" class="field-input birthday-admin-input" placeholder="YYYY-MM-DD" />
            <button class="ghost-button mini-button" @click="saveUserBirthday(user.id)">Save Birthday</button>
            <button class="ghost-button mini-button" @click="resetLotteryChance(user.id)">Add Draw</button>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'growth'" class="panel">
        <view class="section-head-row">
          <view><text class="section-title">VIP cash benefit settings</text><text class="row-meta">Amounts are maintained in NGN and converted to each user's bound currency.</text></view>
          <switch :checked="benefitConfigForm.supportRewardEnabled" color="#002FA7" @change="handleSupportRewardToggle" />
        </view>
        <view class="benefit-config-grid">
          <view><text class="row-meta">VIP4 monthly support red packet (NGN)</text><input v-model="benefitConfigForm.vip4SupportAmountNgn" class="field-input" type="number" /></view>
          <view><text class="row-meta">VIP5 monthly support red packet (NGN)</text><input v-model="benefitConfigForm.vip5SupportAmountNgn" class="field-input" type="number" /></view>
        </view>
        <button class="primary-button benefit-save-button" @click="saveBenefitConfig">Save benefit settings</button>
      </view>

      <view v-if="activeTab === 'growth'" class="panel">
        <view class="section-head-row"><view><text class="section-title">VIP5 country holidays</text><text class="row-meta">One reward per configured holiday and bound country.</text></view><button class="ghost-button mini-button" @click="resetHolidayForm">New holiday</button></view>
        <view class="holiday-form">
          <input v-model="holidayForm.countryCode" class="field-input" placeholder="Country code, e.g. NG" />
          <input v-model="holidayForm.holidayCode" class="field-input" placeholder="Unique code, e.g. independence-day" />
          <input v-model="holidayForm.holidayName" class="field-input" placeholder="Holiday name" />
          <input v-model="holidayForm.holidayDate" class="field-input" placeholder="YYYY-MM-DD" />
          <input v-model="holidayForm.rewardAmount" class="field-input" type="number" placeholder="Reward amount in country currency" />
          <label class="holiday-enabled"><switch :checked="holidayForm.enabled" color="#002FA7" @change="handleHolidayEnabledToggle" /><text>Enabled</text></label>
        </view>
        <button class="primary-button benefit-save-button" @click="saveHoliday">{{ holidayForm.id ? 'Update holiday' : 'Add holiday' }}</button>
        <view v-for="holiday in vipHolidays" :key="holiday.id" class="list-row">
          <view><text class="row-title">{{ holiday.holidayName }}</text><text class="row-meta">{{ holiday.countryCode }} / {{ holiday.holidayDate }} / {{ holiday.rewardAmount }} {{ holiday.currencyCode }}</text></view>
          <view class="row-actions"><text :class="['status-pill', holiday.enabled ? 'active' : 'paused']">{{ holiday.enabled ? 'Enabled' : 'Disabled' }}</text><button class="ghost-button mini-button" @click="editHoliday(holiday)">Edit</button></view>
        </view>
        <text v-if="vipHolidays.length === 0" class="row-meta">No country holidays configured.</text>
      </view>

      <view v-if="activeTab === 'growth'" class="panel">
        <view class="section-head-row"><view><text class="section-title">Benefit claim review</text><text class="row-meta">Support red packets enter the wallet only after approval.</text></view><text class="section-count">{{ pendingBenefitClaims.length }}</text></view>
        <view v-for="claim in vipBenefitClaims" :key="claim.id" class="list-row">
          <view><text class="row-title">{{ claim.username }} / {{ benefitTypeLabel(claim.benefitType) }}</text><text class="row-meta">{{ claim.vipLevel }} / {{ claim.localAmount }} {{ claim.currencyCode }} / {{ claim.periodKey }}</text><text v-if="claim.reviewNote" class="row-meta">{{ claim.reviewNote }}</text></view>
          <view v-if="claim.status === 'pending'" class="row-actions"><button class="ghost-button mini-button" @click="reviewBenefit(claim.id, 'rejected')">Reject</button><button class="primary-button mini-button" @click="reviewBenefit(claim.id, 'approved')">Approve</button></view>
          <text v-else :class="['status-pill', claim.status === 'approved' ? 'active' : 'danger']">{{ claim.status }}</text>
        </view>
        <text v-if="vipBenefitClaims.length === 0" class="row-meta">No benefit claims yet.</text>
      </view>

      <view v-if="activeTab === 'growth'" class="panel">
        <text class="section-title">Lottery prize pool</text>
        <view style="height: 18rpx"></view>
        <view class="prize-grid">
          <view v-for="prize in lotteryPrizes" :key="prize.id" class="prize-card">
            <text class="row-title">{{ prize.name }}</text>
            <text class="row-meta">{{ prize.prizeType }} / weight {{ prize.weight }} / sort {{ prize.sortOrder }}</text>
            <text :class="['status-pill', prize.enabled ? 'active' : 'paused']">{{ prize.enabled ? 'Enabled' : 'Disabled' }}</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'growth'" class="panel">
        <view class="section-head-row">
          <view>
            <text class="section-title">Lottery records</text>
            <text class="row-meta">{{ pendingLotteryCount }} pending fulfillment</text>
          </view>
          <button class="ghost-button mini-button" @click="refreshGrowthData">Refresh</button>
        </view>
        <view style="height: 18rpx"></view>
        <view v-for="record in lotteryRecords" :key="record.id" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ record.username }} / {{ record.prizeName }}</text>
              <text class="row-meta">{{ record.vipLevel }} / {{ record.periodType }} {{ record.periodKey }} / {{ record.drawnAt }}</text>
              <text class="row-meta">Processed by {{ record.processedBy || '-' }} / {{ record.processedAt || '-' }}</text>
            </view>
            <text :class="['status-pill', lotteryStatusClass(record.fulfillmentStatus)]">{{ record.fulfillmentStatus }}</text>
          </view>
          <view class="broadcast-type-row">
            <button
              v-for="status in lotteryStatuses"
              :key="`${record.id}-${status}`"
              class="ghost-button mini-button"
              @click="updateLotteryStatus(record.id, status)"
            >
              {{ status }}
            </button>
          </view>
        </view>
        <text v-if="lotteryRecords.length === 0" class="row-meta">No lottery records yet.</text>
      </view>

      <view v-if="activeTab === 'broadcast'" class="panel">
        <text class="section-title">Broadcast records</text>
        <view style="height: 18rpx"></view>
        <view v-for="item in broadcasts" :key="item.id" class="list-row">
          <view>
            <text class="row-title">{{ item.senderUsername }} / {{ item.scope }}</text>
            <text class="row-meta">{{ item.messageType }} / delivered {{ item.deliveredCount }} / {{ item.createdAt }}</text>
            <text class="row-meta">filters: {{ item.countryCodes || 'all countries' }} / {{ item.searchKeyword || 'no keyword' }} / {{ item.targetMode }}</text>
            <text v-if="item.targetUsernames" class="row-meta">targets: {{ item.targetUsernames }}</text>
            <image v-if="item.messageType === 'image' && item.mediaUrl" class="broadcast-record-image" :src="resolveMediaUrl(item.mediaUrl)" mode="aspectFit" />
            <video v-if="item.messageType === 'video' && item.mediaUrl" class="broadcast-record-video" :src="resolveMediaUrl(item.mediaUrl)" controls />
            <text class="row-meta">{{ item.content }}</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'orders'" class="panel">
        <text class="section-title">Sell and trade orders</text>
        <view style="height: 18rpx"></view>
        <view v-for="order in transactions" :key="order.id" class="conversation-card">
          <view class="list-row compact-row">
            <view class="admin-order-identity">
              <image class="admin-order-logo" :src="cardLogoFor(order.cardName)" mode="aspectFit" />
              <view>
                <text class="row-title">{{ order.orderNo }} / {{ order.cardName }}</text>
                <text class="row-meta">{{ order.faceValue }} / {{ order.payoutAmount }} / {{ order.counterpartyUsername }}</text>
                <text class="row-meta">{{ order.note }}</text>
              </view>
            </view>
            <text :class="['status-pill', order.status === 'completed' ? 'active' : 'warning']">{{ order.status }}</text>
          </view>
          <view class="broadcast-type-row">
            <button
              v-for="status in transactionStatuses(order.status)"
              :key="`${order.id}-${status}`"
              class="ghost-button mini-button"
              @click="updateOrderStatus(order.id, status)"
            >
              {{ status }}
            </button>
            <button
              v-if="order.status === 'pending' || order.status === 'processing'"
              class="primary-button mini-button"
              @click="completeOrderAtEstimate(order)"
            >
              Complete at estimate
            </button>
            <button
              v-if="order.status === 'pending' || order.status === 'processing'"
              class="ghost-button mini-button danger-action"
              @click="cancelOrder(order.id)"
            >
              Cancel bad card
            </button>
          </view>
          <text v-if="order.status === 'canceled'" class="row-meta">Canceled: {{ order.cancelReason }} {{ order.cancelNote }}</text>
        </view>
      </view>

      <view v-if="activeTab === 'withdrawals'" class="panel">
        <text class="section-title">Withdrawals and prize claims</text>
        <view style="height: 18rpx"></view>
        <view v-for="item in withdrawals" :key="item.id" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="claim-type">{{ item.sourceType === 'lottery_cash' ? 'Lottery cash claim' : 'Wallet withdrawal' }}</text>
              <text class="row-title">{{ item.requestNo }} / {{ item.amount }}</text>
              <text v-if="item.sourceType === 'lottery_cash'" class="row-meta">{{ item.prizeName }} / Lottery record {{ item.lotteryRecordId }}</text>
              <text class="row-meta">{{ item.ownerUsername || item.accountName }} / {{ item.bankName }} / {{ item.accountNumber }}</text>
              <text class="row-meta">{{ item.assignedAgent }} / {{ item.createdAt }}</text>
            </view>
            <text :class="['status-pill', item.status === 'completed' ? 'active' : item.status === 'rejected' ? 'danger' : 'warning']">{{ item.status }}</text>
          </view>
          <view v-if="item.status === 'pending'" class="row-actions">
            <button class="ghost-button mini-button" @click="completeWithdrawal(item.id)">Mark Completed</button>
            <button v-if="item.sourceType === 'wallet'" class="ghost-button mini-button danger-action" @click="rejectWithdrawal(item.id)">Reject</button>
          </view>
        </view>
        <view v-for="item in lotteryFulfillments" :key="item.id" class="conversation-card physical-claim-card">
          <view class="list-row compact-row">
            <view>
              <text class="claim-type physical">Physical prize delivery</text>
              <text class="row-title">{{ item.orderNo }} / {{ item.prizeName }}</text>
              <text class="row-meta">{{ item.ownerUsername }} / Lottery record {{ item.lotteryRecordId }}</text>
              <text class="row-meta">{{ item.recipientName }} / {{ item.phone }} / {{ item.country }}</text>
              <text class="row-meta address-meta">{{ item.addressLine }}</text>
              <text class="row-meta">{{ item.assignedAgent }} / {{ item.createdAt }}</text>
            </view>
            <text :class="['status-pill', item.status === 'completed' ? 'active' : 'warning']">{{ item.status }}</text>
          </view>
          <button
            v-if="item.status !== 'completed'"
            class="ghost-button mini-button"
            @click="completeLotteryFulfillment(item.id)"
          >
            Mark Delivered
          </button>
        </view>
        <text v-if="withdrawals.length === 0 && lotteryFulfillments.length === 0" class="row-meta">No withdrawal or prize claims.</text>
      </view>

      <view v-if="activeTab === 'risks'" class="panel">
        <text class="section-title">银行卡绑定记录</text>
        <view style="height: 18rpx"></view>
        <view v-for="account in bankAccounts" :key="account.id" class="list-row">
          <view>
            <text class="row-title">{{ account.ownerUsername }} / {{ account.status }}</text>
            <text class="row-meta">{{ account.country }} / {{ account.bankName }} / {{ account.accountName }}</text>
            <text class="row-meta">{{ account.maskedAccountNumber }} / {{ account.createdAt }}</text>
          </view>
          <text class="status-pill active">已绑定</text>
        </view>
        <text v-if="bankAccounts.length === 0" class="row-meta">暂无银行卡绑定记录。</text>
      </view>

      <view v-if="activeTab === 'risks'" class="panel">
        <text class="section-title">重复银行卡风控</text>
        <view style="height: 18rpx"></view>
        <view v-for="risk in bankAccountRisks" :key="`${risk.username}-${risk.reason}-${risk.accountNumber}-${risk.submittedAt}`" class="list-row">
          <view>
            <text class="row-title">{{ risk.username }} / {{ risk.riskLevel }}</text>
            <text class="row-meta">{{ risk.reason }} / {{ risk.bankName }} / {{ risk.accountName }}</text>
            <text class="row-meta">{{ risk.accountNumber }} / {{ risk.phoneCountryCode || '-' }} / {{ risk.assignedAgent || '-' }}</text>
            <text class="row-meta">{{ risk.submittedAt }}</text>
          </view>
          <text :class="['status-pill', risk.riskLevel === 'high' ? 'danger' : 'warning']">{{ risk.riskLevel }}</text>
        </view>
        <text v-if="bankAccountRisks.length === 0" class="row-meta">暂无重复银行卡风险。</text>
      </view>

      <view v-if="activeTab === 'loans'" class="panel">
        <text class="section-title">Loan applications</text>
        <view style="height: 18rpx"></view>
        <view v-for="loan in loans" :key="loan.id" class="conversation-card">
          <view class="list-row compact-row">
            <view>
              <text class="row-title">{{ loan.applicationNo }} / {{ loan.ownerUsername }}</text>
              <text class="row-meta">{{ loan.amount }} / {{ loan.country }} / {{ loan.assignedAgent }}</text>
              <text class="row-meta">{{ loan.purpose }}</text>
              <text v-if="loan.reviewNote" class="row-meta">{{ loan.reviewNote }}</text>
            </view>
            <text :class="['status-pill', loan.status === 'approved' ? 'active' : loan.status === 'rejected' ? 'danger' : 'warning']">
              {{ loan.status }}
            </text>
          </view>
          <input v-model="loanReviewDrafts[loan.id]" class="field-input assign-input" placeholder="Review note" />
          <view class="broadcast-type-row">
            <button class="ghost-button mini-button" @click="reviewLoan(loan.id, 'approved')">Approve</button>
            <button class="ghost-button mini-button danger-action" @click="reviewLoan(loan.id, 'rejected')">Reject</button>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'rewards'" class="panel">
        <text class="section-title">Referral rewards</text>
        <view style="height: 18rpx"></view>
        <view class="reward-setting-row">
          <view>
            <text class="row-title">Registration cashback</text>
            <text class="row-meta">Fixed reward paid to the inviter after a new user registers.</text>
          </view>
          <switch :checked="rewardForm.registrationCashbackEnabled" @change="setRegistrationRewardEnabled" />
        </view>
        <input v-model="rewardForm.registrationCashbackAmount" class="field-input" type="digit" placeholder="Registration cashback amount" />
        <view style="height: 18rpx"></view>
        <view class="reward-setting-row">
          <view>
            <text class="row-title">Trade rebate</text>
            <text class="row-meta">Percent reward paid to the inviter when the new user's order is completed.</text>
          </view>
          <switch :checked="rewardForm.tradeRebateEnabled" @change="setTradeRewardEnabled" />
        </view>
        <input v-model="rewardForm.tradeRebatePercent" class="field-input" type="digit" placeholder="Trade rebate percent" />
        <view style="height: 18rpx"></view>
        <button class="primary-button" @click="saveRewardConfig">Save Reward Rules</button>
        <view style="height: 12rpx"></view>
        <text class="row-meta">Updated by {{ referralRewardConfig?.updatedBy || '-' }} / {{ referralRewardConfig?.updatedAt || '-' }}</text>
      </view>

      <view v-if="activeTab === 'rewards'" class="panel">
        <text class="section-title">Reward records</text>
        <view style="height: 18rpx"></view>
        <view v-for="reward in referralRewards" :key="reward.id" class="list-row">
          <view>
            <text class="row-title">{{ reward.referrerUsername }} earned {{ reward.amount }}</text>
            <text class="row-meta">{{ reward.rewardType }} / invited {{ reward.referredUsername }} / {{ reward.status }}</text>
            <text class="row-meta">{{ reward.tradeOrderNo || 'Registration reward' }} / {{ reward.createdAt }}</text>
          </view>
          <text :class="['status-pill', reward.status === 'available' ? 'active' : 'warning']">
            {{ reward.ratePercent ? reward.ratePercent + '%' : 'fixed' }}
          </text>
        </view>
      </view>

      <view v-if="activeTab === 'rewards'" class="panel">
        <text class="section-title">Registration bonus by phone country code</text>
        <view style="height: 18rpx"></view>
        <view v-for="config in registrationBonusConfigs" :key="config.countryCode" class="bonus-config-card">
          <view class="reward-setting-row">
            <view>
              <text class="row-title">{{ config.countryCode }} / {{ registrationBonusDrafts[config.countryCode]?.countryName || config.countryName }}</text>
              <text class="row-meta">Paid once when a new account registers with this phone prefix.</text>
            </view>
            <switch
              :checked="registrationBonusDrafts[config.countryCode]?.enabled"
              @change="setBonusEnabled(config.countryCode, $event)"
            />
          </view>
          <view class="bonus-config-grid">
            <input v-model="registrationBonusDrafts[config.countryCode].countryName" class="field-input" placeholder="Country name" />
            <input v-model="registrationBonusDrafts[config.countryCode].currencyCode" class="field-input" placeholder="Currency" />
            <input v-model="registrationBonusDrafts[config.countryCode].bonusAmount" class="field-input" type="digit" placeholder="Bonus amount" />
          </view>
          <input v-model="registrationBonusDrafts[config.countryCode].note" class="field-input" placeholder="Internal note" />
          <view style="height: 12rpx"></view>
          <button class="primary-button mini-button" @click="saveRegistrationBonusConfig(config)">Save {{ config.countryCode }}</button>
          <text class="row-meta">Updated by {{ config.updatedBy || '-' }} / {{ config.updatedAt || '-' }}</text>
        </view>
      </view>

      <view v-if="activeTab === 'rewards'" class="panel">
        <text class="section-title">Registration bonus records</text>
        <view style="height: 18rpx"></view>
        <view v-for="record in registrationBonusRecords" :key="record.id" class="list-row">
          <view>
            <text class="row-title">{{ record.username }} received {{ record.bonusAmount }} {{ record.currencyCode }}</text>
            <text class="row-meta">{{ record.countryCode }} / {{ record.phone || 'No phone' }} / {{ record.status }}</text>
            <text class="row-meta">{{ record.reason }} / {{ record.createdAt }}</text>
          </view>
        </view>
      </view>

      <view v-if="activeTab === 'notifications'" class="panel">
        <text class="section-title">Admin notifications</text>
        <view style="height: 18rpx"></view>
        <view v-for="item in notifications" :key="item.id" class="list-row">
          <view>
            <text class="row-title">{{ item.title }}</text>
            <text class="row-meta">{{ item.eventType }} / {{ item.targetType }} / {{ item.createdAt }}</text>
            <text class="row-meta">{{ item.body }}</text>
          </view>
        </view>
      </view>

      <text v-if="notice" class="notice-text">{{ notice }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, reactive, ref } from 'vue'
import { cardLogoFor } from '@/utils/art'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import type {
  AdminDirectConversationItem,
  AdminUserItem,
  AgentItem,
  BankAccountItem,
  BankAccountRiskMatch,
  BroadcastItem,
  LoanApplicationItem,
  LotteryFulfillmentItem,
  LotteryPrizeItem,
  LotteryRecordItem,
  NotificationItem,
  ReferralRewardConfigItem,
  ReferralRewardItem,
  RegistrationBonusConfigItem,
  RegistrationBonusRecordItem,
  SupportConversationItem,
  TransactionItem,
  VipBenefitClaimItem,
  VipBenefitConfigItem,
  VipHolidayRewardItem,
  WithdrawalItem
} from '@/types'
import {
  assignSupportConversation,
  cancelTransaction,
  completeTransaction,
  createBroadcast,
  createAgent,
  fetchAdminDirectConversations,
  fetchAdminBankAccounts,
  fetchAdminBankAccountRisks,
  fetchAdminLotteryRecords,
  fetchAdminSupportConversations,
  fetchAdminUsers,
  fetchAdminVipBenefitConfig,
  fetchAdminVipHolidays,
  fetchAgents,
  fetchBroadcasts,
  fetchLoans,
  fetchLotteryFulfillments,
  fetchNotifications,
  fetchReferralRewardConfig,
  fetchReferralRewards,
  fetchRegistrationBonusConfigs,
  fetchRegistrationBonusRecords,
  fetchLotteryPrizes,
  fetchTransactions,
  fetchWithdrawals,
  fetchStaffVipBenefitClaims,
  resetAdminLotteryEligibility,
  updateAgentStatus,
  updateAgentWelcomeMessage,
  updateAdminLotteryRecordStatus,
  updateAdminUserBirthday,
  updateAdminVipBenefitConfig,
  updateLoanStatus,
  updateReferralRewardConfig,
  updateRegistrationBonusConfig,
  updateTransactionStatus,
  updateLotteryFulfillmentStatus,
  updateWithdrawalStatus,
  uploadImage,
  uploadVideo,
  reviewVipBenefitClaim,
  saveAdminVipHoliday
} from '@/utils/api'
import { useAppStore } from '@/store/app'

const store = useAppStore()
const isAdminReady = ref(false)
const activeTab = ref<'users' | 'growth' | 'agents' | 'support' | 'direct' | 'broadcast' | 'orders' | 'withdrawals' | 'risks' | 'loans' | 'rewards' | 'notifications'>('users')
const notice = ref('')
const users = ref<AdminUserItem[]>([])
const agents = ref<AgentItem[]>([])
const conversations = ref<SupportConversationItem[]>([])
const directConversations = ref<AdminDirectConversationItem[]>([])
const broadcasts = ref<BroadcastItem[]>([])
const transactions = ref<TransactionItem[]>([])
const withdrawals = ref<WithdrawalItem[]>([])
const lotteryFulfillments = ref<LotteryFulfillmentItem[]>([])
const bankAccounts = ref<BankAccountItem[]>([])
const bankAccountRisks = ref<BankAccountRiskMatch[]>([])
const loans = ref<LoanApplicationItem[]>([])
const lotteryRecords = ref<LotteryRecordItem[]>([])
const lotteryPrizes = ref<LotteryPrizeItem[]>([])
const vipBenefitConfig = ref<VipBenefitConfigItem | null>(null)
const vipHolidays = ref<VipHolidayRewardItem[]>([])
const vipBenefitClaims = ref<VipBenefitClaimItem[]>([])
const referralRewardConfig = ref<ReferralRewardConfigItem | null>(null)
const referralRewards = ref<ReferralRewardItem[]>([])
const registrationBonusConfigs = ref<RegistrationBonusConfigItem[]>([])
const registrationBonusRecords = ref<RegistrationBonusRecordItem[]>([])
const notifications = ref<NotificationItem[]>([])
const directSearch = ref('')
const assignDrafts = reactive<Record<string, string>>({})
const welcomeDrafts = reactive<Record<string, { content: string; enabled: boolean }>>({})
const editingWelcomeAgentId = ref('')
const loanReviewDrafts = reactive<Record<string, string>>({})
const birthdayDrafts = reactive<Record<string, string>>({})
const registrationBonusDrafts = reactive<Record<string, {
  countryName: string
  currencyCode: string
  bonusAmount: string
  enabled: boolean
  note: string
}>>({})
const broadcastTypes = ['text', 'image', 'video', 'voice', 'gif', 'link'] as const
const adminBroadcastCountryOptions = ['+234', '+91', '+237', '+233', '+254']
const lotteryStatuses = ['pending', 'processing', 'fulfilled', 'canceled']
const vipRules = [
  { level: 'VIP0', threshold: 'New account', draw: 'One permanent registration draw' },
  { level: 'VIP1', threshold: 'First completed trade', draw: 'One permanent upgrade draw' },
  { level: 'VIP2', threshold: 'USD 1,000 lifetime', draw: 'One draw each calendar month' },
  { level: 'VIP3', threshold: 'USD 5,000 lifetime', draw: 'One draw per half-month' },
  { level: 'VIP4', threshold: 'USD 10,000 lifetime', draw: 'One weekly draw, birthday reward, monthly support reward, loans' },
  { level: 'VIP5', threshold: 'USD 50,000 lifetime', draw: 'One weekly draw, larger rewards, loans, country holidays' }
]

const benefitConfigForm = reactive({ vip4SupportAmountNgn: '0', vip5SupportAmountNgn: '0', supportRewardEnabled: true })
const holidayForm = reactive({ id: '', countryCode: '', holidayCode: '', holidayName: '', holidayDate: '', rewardAmount: '', enabled: true })

const broadcastForm = reactive({
  content: '',
  messageType: 'text' as BroadcastItem['messageType'],
  mediaUrl: '',
  mediaName: '',
  uploading: false,
  keyword: '',
  countryCodes: [] as string[]
})

const agentForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: ''
})

const rewardForm = reactive({
  registrationCashbackEnabled: true,
  registrationCashbackAmount: '1.00',
  tradeRebateEnabled: true,
  tradeRebatePercent: '5'
})

onShow(() => {
  if (requireAdmin()) {
    refreshAll()
  }
})

function requireAdmin() {
  if (store.state.currentUser?.roleCode === 'ADMIN') {
    isAdminReady.value = true
    return true
  }
  isAdminReady.value = false
  notice.value = 'Admin account required.'
  uni.redirectTo({ url: '/pages/admin-login/index' })
  return false
}

function goSupportChat() {
  uni.redirectTo({ url: '/pages/support-chat-v2/index' })
}

function goSupportConversation(conversationId: string) {
  uni.redirectTo({ url: `/pages/support-chat-v2/index?conversationId=${encodeURIComponent(conversationId)}` })
}

function goRateAdmin() {
  uni.redirectTo({ url: '/pages/admin-rates/index' })
}

function goUserHome() {
  uni.redirectTo({ url: '/pages/home/index' })
}

async function refreshAll() {
  try {
    const [
      nextUsers,
      nextAgents,
      nextConversations,
      nextBroadcasts,
      nextTransactions,
      nextWithdrawals,
      nextLotteryFulfillments,
      nextBankAccounts,
      nextBankAccountRisks,
      nextLoans,
      nextLotteryRecords,
      nextLotteryPrizes,
      nextRewardConfig,
      nextRewards,
      nextRegistrationBonusConfigs,
      nextRegistrationBonusRecords,
      nextNotifications,
      nextBenefitConfig,
      nextVipHolidays,
      nextVipBenefitClaims
    ] = await Promise.all([
      fetchAdminUsers(),
      fetchAgents(),
      fetchAdminSupportConversations(),
      fetchBroadcasts(),
      fetchTransactions(),
      fetchWithdrawals(),
      fetchLotteryFulfillments(),
      fetchAdminBankAccounts(),
      fetchAdminBankAccountRisks(),
      fetchLoans(),
      fetchAdminLotteryRecords(),
      fetchLotteryPrizes(),
      fetchReferralRewardConfig(),
      fetchReferralRewards(),
      fetchRegistrationBonusConfigs(),
      fetchRegistrationBonusRecords(),
      fetchNotifications(),
      fetchAdminVipBenefitConfig(),
      fetchAdminVipHolidays(),
      fetchStaffVipBenefitClaims()
    ])
    users.value = nextUsers
    agents.value = nextAgents
    conversations.value = nextConversations
    broadcasts.value = nextBroadcasts
    transactions.value = nextTransactions
    withdrawals.value = nextWithdrawals
    lotteryFulfillments.value = nextLotteryFulfillments
    bankAccounts.value = nextBankAccounts
    bankAccountRisks.value = nextBankAccountRisks
    loans.value = nextLoans
    lotteryRecords.value = nextLotteryRecords
    lotteryPrizes.value = nextLotteryPrizes
    referralRewardConfig.value = nextRewardConfig
    referralRewards.value = nextRewards
    registrationBonusConfigs.value = nextRegistrationBonusConfigs
    registrationBonusRecords.value = nextRegistrationBonusRecords
    applyRewardConfig(nextRewardConfig)
    applyRegistrationBonusConfigs(nextRegistrationBonusConfigs)
    notifications.value = nextNotifications
    vipBenefitConfig.value = nextBenefitConfig
    vipHolidays.value = nextVipHolidays
    vipBenefitClaims.value = nextVipBenefitClaims
    applyBenefitConfig(nextBenefitConfig)
    applyWelcomeDrafts(nextAgents)
    await store.refreshBalanceSummary().catch(() => {})
    nextConversations.forEach((conversation) => {
      assignDrafts[conversation.conversationId] = conversation.assignedAgent || ''
    })
    nextLoans.forEach((loan) => {
      loanReviewDrafts[loan.id] = loan.reviewNote || ''
    })
    if (!directConversations.value.length) {
      directConversations.value = await fetchAdminDirectConversations()
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Admin data failed'
  }
}

function applyRewardConfig(config: ReferralRewardConfigItem) {
  rewardForm.registrationCashbackEnabled = config.registrationCashbackEnabled
  rewardForm.registrationCashbackAmount = config.registrationCashbackAmount
  rewardForm.tradeRebateEnabled = config.tradeRebateEnabled
  rewardForm.tradeRebatePercent = config.tradeRebatePercent
}

function applyRegistrationBonusConfigs(configs: RegistrationBonusConfigItem[]) {
  configs.forEach((config) => {
    registrationBonusDrafts[config.countryCode] = {
      countryName: config.countryName,
      currencyCode: config.currencyCode,
      bonusAmount: config.bonusAmount,
      enabled: config.enabled,
      note: config.note
    }
  })
}

function applyWelcomeDrafts(nextAgents: AgentItem[]) {
  nextAgents.forEach((agent) => {
    welcomeDrafts[agent.id] = {
      content: agent.welcomeMessage || '',
      enabled: agent.welcomeMessageEnabled
    }
  })
}

function setRegistrationRewardEnabled(event: Event) {
  rewardForm.registrationCashbackEnabled = switchValue(event)
}

function setTradeRewardEnabled(event: Event) {
  rewardForm.tradeRebateEnabled = switchValue(event)
}

function setWelcomeEnabled(agentId: string, event: Event) {
  const draft = welcomeDrafts[agentId]
  if (draft) {
    draft.enabled = switchValue(event)
  }
}

function setBonusEnabled(countryCode: string, event: Event) {
  const draft = registrationBonusDrafts[countryCode]
  if (draft) {
    draft.enabled = switchValue(event)
  }
}

function switchValue(event: Event) {
  return Boolean((event as unknown as { detail?: { value?: boolean } }).detail?.value)
}

async function saveRewardConfig() {
  try {
    const updated = await updateReferralRewardConfig({
      registrationCashbackEnabled: rewardForm.registrationCashbackEnabled,
      registrationCashbackAmount: rewardForm.registrationCashbackAmount,
      tradeRebateEnabled: rewardForm.tradeRebateEnabled,
      tradeRebatePercent: rewardForm.tradeRebatePercent
    })
    referralRewardConfig.value = updated
    applyRewardConfig(updated)
    notice.value = 'Reward rules saved.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Reward rules failed'
  }
}

async function saveRegistrationBonusConfig(config: RegistrationBonusConfigItem) {
  try {
    const draft = registrationBonusDrafts[config.countryCode]
    if (!draft) return
    const updated = await updateRegistrationBonusConfig({
      countryCode: config.countryCode,
      countryName: draft.countryName,
      currencyCode: draft.currencyCode,
      bonusAmount: draft.bonusAmount,
      enabled: draft.enabled,
      note: draft.note
    })
    const index = registrationBonusConfigs.value.findIndex(item => item.countryCode === updated.countryCode)
    if (index >= 0) {
      registrationBonusConfigs.value.splice(index, 1, updated)
    }
    applyRegistrationBonusConfigs(registrationBonusConfigs.value)
    notice.value = `${config.countryCode} registration bonus saved.`
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Registration bonus save failed'
  }
}

async function submitBroadcast() {
  try {
    const content = broadcastForm.content.trim()
    const isMedia = broadcastForm.messageType === 'image' || broadcastForm.messageType === 'video'
    if (isMedia && !broadcastForm.mediaUrl) {
      notice.value = 'Choose an image or video before sending.'
      return
    }
    if (!isMedia && !content) return
    const confirmed = await confirmAdminBroadcast(content)
    if (!confirmed) return
    await createBroadcast({
      scope: 'all',
      content,
      messageType: broadcastForm.messageType,
      mediaUrl: broadcastForm.mediaUrl || undefined,
      countryCodes: broadcastForm.countryCodes,
      keyword: broadcastForm.keyword.trim()
    })
    broadcastForm.content = ''
    clearAdminBroadcastMedia()
    broadcastForm.keyword = ''
    notice.value = 'Broadcast sent.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Broadcast failed'
  }
}

function confirmAdminBroadcast(content: string) {
  const countryText = broadcastForm.countryCodes.length ? broadcastForm.countryCodes.join(', ') : 'all countries'
  const keywordText = broadcastForm.keyword.trim() || 'no keyword'
  const countText = `${adminBroadcastTargetCount.value} estimated targets`
  return new Promise<boolean>((resolve) => {
    uni.showModal({
      title: 'Confirm broadcast',
      content: `Countries: ${countryText}\nKeyword: ${keywordText}\nTargets: ${countText}\nMedia: ${broadcastForm.mediaUrl ? broadcastForm.messageType : 'none'}\nMessage: ${content || '(no caption)'}`,
      confirmText: 'Send',
      cancelText: 'Cancel',
      success(result) {
        resolve(Boolean(result.confirm))
      },
      fail() {
        resolve(false)
      }
    })
  })
}

function setAdminBroadcastType(type: BroadcastItem['messageType']) {
  broadcastForm.messageType = type
  if (type !== 'image' && type !== 'video') {
    clearAdminBroadcastMedia(false)
  }
}

function chooseAdminBroadcastImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success(result) {
      const filePath = result.tempFilePaths[0]
      if (filePath) uploadAdminBroadcastMedia('image', filePath, 'Broadcast image')
    }
  })
}

function chooseAdminBroadcastVideo() {
  uni.chooseVideo({
    sourceType: ['album', 'camera'],
    compressed: false,
    success(result) {
      if (result.tempFilePath) uploadAdminBroadcastMedia('video', result.tempFilePath, 'Broadcast video')
    }
  })
}

async function uploadAdminBroadcastMedia(type: 'image' | 'video', filePath: string, fallbackName: string) {
  broadcastForm.uploading = true
  notice.value = `Uploading ${type}...`
  try {
    const asset = type === 'image' ? await uploadImage(filePath) : await uploadVideo(filePath)
    broadcastForm.messageType = type
    broadcastForm.mediaUrl = asset.publicUrl
    broadcastForm.mediaName = asset.originalName || fallbackName
    notice.value = `${type === 'image' ? 'Image' : 'Video'} ready.`
  } catch (error) {
    notice.value = error instanceof Error ? error.message : `${fallbackName} upload failed`
  } finally {
    broadcastForm.uploading = false
  }
}

function clearAdminBroadcastMedia(resetType = true) {
  broadcastForm.mediaUrl = ''
  broadcastForm.mediaName = ''
  if (resetType && (broadcastForm.messageType === 'image' || broadcastForm.messageType === 'video')) {
    broadcastForm.messageType = 'text'
  }
}

const adminBroadcastTargetCount = computed(() => {
  const keyword = broadcastForm.keyword.trim().toLowerCase()
  const countries = broadcastForm.countryCodes
  return users.value.filter((user) => {
    if (user.role !== 'USER' || user.status !== 'ACTIVE') return false
    const country = resolveAdminUserCountry(user.phone)
    if (countries.length > 0 && !countries.includes(country)) return false
    if (!keyword) return true
    return [user.username, user.email, user.phone, country]
      .some(value => (value || '').toLowerCase().includes(keyword))
  }).length
})

const userVipRows = computed(() => users.value
  .filter(user => user.role === 'USER')
  .slice()
  .sort((left, right) => {
    const levelDiff = vipLevelWeight(right.vipLevel) - vipLevelWeight(left.vipLevel)
    if (levelDiff !== 0) return levelDiff
    return Number(right.vipPoints || 0) - Number(left.vipPoints || 0)
  }))

const pendingLotteryCount = computed(() => lotteryRecords.value
  .filter(record => record.fulfillmentStatus === 'pending')
  .length)
const pendingBenefitClaims = computed(() => vipBenefitClaims.value.filter(claim => claim.status === 'pending'))

function vipCount(level: string) {
  return users.value.filter(user => user.role === 'USER' && user.vipLevel === level).length
}

function vipLevelWeight(level: string) {
  return { VIP0: 0, VIP1: 1, VIP2: 2, VIP3: 3, VIP4: 4, VIP5: 5 }[level as 'VIP0' | 'VIP1' | 'VIP2' | 'VIP3' | 'VIP4' | 'VIP5'] || 0
}

function applyBenefitConfig(config: VipBenefitConfigItem) {
  benefitConfigForm.vip4SupportAmountNgn = config.vip4SupportAmountNgn
  benefitConfigForm.vip5SupportAmountNgn = config.vip5SupportAmountNgn
  benefitConfigForm.supportRewardEnabled = config.supportRewardEnabled
}

function handleSupportRewardToggle(event: Event) {
  benefitConfigForm.supportRewardEnabled = switchValue(event)
}

function handleHolidayEnabledToggle(event: Event) {
  holidayForm.enabled = switchValue(event)
}

async function saveBenefitConfig() {
  try {
    const config = await updateAdminVipBenefitConfig({
      vip4SupportAmountNgn: Number(benefitConfigForm.vip4SupportAmountNgn),
      vip5SupportAmountNgn: Number(benefitConfigForm.vip5SupportAmountNgn),
      supportRewardEnabled: benefitConfigForm.supportRewardEnabled
    })
    vipBenefitConfig.value = config
    applyBenefitConfig(config)
    notice.value = 'VIP benefit settings saved.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Benefit settings update failed'
  }
}

function resetHolidayForm() {
  Object.assign(holidayForm, { id: '', countryCode: '', holidayCode: '', holidayName: '', holidayDate: '', rewardAmount: '', enabled: true })
}

function editHoliday(holiday: VipHolidayRewardItem) {
  Object.assign(holidayForm, {
    id: holiday.id,
    countryCode: holiday.countryCode,
    holidayCode: holiday.holidayCode,
    holidayName: holiday.holidayName,
    holidayDate: holiday.holidayDate,
    rewardAmount: holiday.rewardAmount,
    enabled: holiday.enabled
  })
}

async function saveHoliday() {
  if (!holidayForm.countryCode.trim() || !holidayForm.holidayCode.trim() || !holidayForm.holidayName.trim() || !holidayForm.holidayDate.trim() || !holidayForm.rewardAmount.trim()) {
    notice.value = 'Complete every holiday field.'
    return
  }
  try {
    await saveAdminVipHoliday({
      id: holidayForm.id || undefined,
      countryCode: holidayForm.countryCode.trim().toUpperCase(),
      holidayCode: holidayForm.holidayCode.trim(),
      holidayName: holidayForm.holidayName.trim(),
      holidayDate: holidayForm.holidayDate.trim(),
      rewardAmount: Number(holidayForm.rewardAmount),
      enabled: holidayForm.enabled
    })
    vipHolidays.value = await fetchAdminVipHolidays()
    resetHolidayForm()
    notice.value = 'Country holiday saved.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Holiday save failed'
  }
}

async function reviewBenefit(claimId: string, status: 'approved' | 'rejected') {
  try {
    const updated = await reviewVipBenefitClaim(claimId, status, status === 'approved' ? 'Approved in admin console' : 'Rejected in admin console')
    const index = vipBenefitClaims.value.findIndex(claim => claim.id === claimId)
    if (index >= 0) vipBenefitClaims.value[index] = updated
    notice.value = `Benefit claim ${status}.`
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Benefit review failed'
  }
}

function benefitTypeLabel(type: string) {
  return { birthday: 'Birthday reward', support_red_packet: 'Support red packet', holiday: 'Holiday reward' }[type] || type
}

async function saveUserBirthday(userId: string) {
  const birthDate = (birthdayDrafts[userId] || '').trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(birthDate)) {
    notice.value = 'Enter the birthday as YYYY-MM-DD.'
    return
  }
  try {
    await updateAdminUserBirthday(userId, birthDate)
    birthdayDrafts[userId] = ''
    notice.value = 'User birthday updated.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Birthday update failed'
  }
}

function lotteryStatusClass(status: string) {
  if (status === 'fulfilled') return 'active'
  if (status === 'canceled') return 'danger'
  return 'warning'
}

function resolveAdminUserCountry(phone: string) {
  const normalized = `+${(phone || '').replace(/^00/, '').replace(/[^0-9]/g, '')}`
  return adminBroadcastCountryOptions
    .slice()
    .sort((left, right) => right.length - left.length)
    .find(code => normalized.startsWith(code)) || ''
}

function toggleAdminBroadcastCountry(code: string) {
  const index = broadcastForm.countryCodes.indexOf(code)
  if (index >= 0) {
    broadcastForm.countryCodes.splice(index, 1)
    return
  }
  broadcastForm.countryCodes.push(code)
}

function transactionStatuses(status: TransactionItem['status']) {
  if (status === 'pending') return ['processing'] as TransactionItem['status'][]
  return [] as TransactionItem['status'][]
}

async function refreshGrowthData() {
  try {
    const [nextUsers, nextRecords, nextPrizes] = await Promise.all([
      fetchAdminUsers(),
      fetchAdminLotteryRecords(),
      fetchLotteryPrizes()
    ])
    users.value = nextUsers
    lotteryRecords.value = nextRecords
    lotteryPrizes.value = nextPrizes
    notice.value = 'Growth data refreshed.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Growth refresh failed'
  }
}

async function updateLotteryStatus(recordId: string, status: string) {
  try {
    await updateAdminLotteryRecordStatus(recordId, status)
    notice.value = `Lottery record moved to ${status}.`
    await refreshGrowthData()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Lottery status update failed'
  }
}

function resetLotteryChance(userId: string) {
  const user = users.value.find(item => item.id === userId)
  uni.showModal({
    title: 'Reset draw chance',
    content: `Add one manual draw chance for ${user?.username || 'this user'}?`,
    confirmText: 'Reset',
    cancelText: 'Cancel',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await resetAdminLotteryEligibility(userId, 'Admin reset from Growth console')
        notice.value = 'Lottery chance reset.'
        await refreshGrowthData()
      } catch (error) {
        notice.value = error instanceof Error ? error.message : 'Lottery reset failed'
      }
    }
  })
}

async function updateOrderStatus(orderId: string, status: TransactionItem['status']) {
  try {
    await updateTransactionStatus(orderId, status)
    notice.value = `Order moved to ${status}.`
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Order update failed'
  }
}

function completeOrderAtEstimate(order: TransactionItem) {
  const rawAmount = order.estimatedLocalAmount || order.localAmount || order.payoutAmount || ''
  const match = String(rawAmount).replace(/,/g, '').match(/-?\d+(?:\.\d+)?/)
  const finalLocalAmount = Number(match?.[0] || '')
  if (!Number.isFinite(finalLocalAmount) || finalLocalAmount <= 0) {
    notice.value = 'This order has no valid estimated payout. Complete it from the support workbench.'
    return
  }

  uni.showModal({
    title: 'Complete order',
    content: `Settle ${order.orderNo} at ${order.currencyCode || ''} ${finalLocalAmount.toLocaleString()}?`,
    confirmText: 'Complete',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await completeTransaction(order.id, { finalLocalAmount, vipPoints: 0 })
        notice.value = `Order ${order.orderNo} completed.`
        await refreshAll()
      } catch (error) {
        notice.value = error instanceof Error ? error.message : 'Order completion failed'
      }
    }
  })
}

async function cancelOrder(orderId: string) {
  try {
    await cancelTransaction(orderId, {
      reason: 'Bad card',
      notifyCustomer: true
    })
    notice.value = 'Order canceled.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Order cancellation failed'
  }
}

async function completeWithdrawal(withdrawalId: string) {
  try {
    await updateWithdrawalStatus(withdrawalId, 'completed')
    notice.value = 'Withdrawal completed.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Withdrawal update failed'
  }
}

async function reviewLoan(loanId: string, status: LoanApplicationItem['status']) {
  try {
    await updateLoanStatus(loanId, status, loanReviewDrafts[loanId] || undefined)
    notice.value = `Loan ${status}.`
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Loan review failed'
  }
}

async function refreshDirectConversations() {
  try {
    directConversations.value = await fetchAdminDirectConversations(directSearch.value)
    notice.value = 'Direct records refreshed.'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Direct records failed'
  }
}

async function submitAgent() {
  try {
    await createAgent({
      username: agentForm.username,
      email: agentForm.email || undefined,
      phone: agentForm.phone || undefined,
      password: agentForm.password
    })
    notice.value = 'Agent created.'
    agentForm.username = ''
    agentForm.email = ''
    agentForm.phone = ''
    agentForm.password = ''
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Create agent failed'
  }
}

async function toggleAgent(agentId: string, status: string) {
  try {
    await updateAgentStatus(agentId, status)
    notice.value = `Agent moved to ${status}.`
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Agent status failed'
  }
}

async function rejectWithdrawal(withdrawalId: string) {
  try {
    await updateWithdrawalStatus(withdrawalId, 'rejected')
    notice.value = 'Withdrawal rejected and reserved funds released.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Withdrawal update failed'
  }
}

async function completeLotteryFulfillment(orderId: string) {
  try {
    await updateLotteryFulfillmentStatus(orderId, 'completed')
    notice.value = 'Prize delivery completed.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Prize delivery update failed'
  }
}

function editWelcomeMessage(agent: AgentItem) {
  if (!welcomeDrafts[agent.id]) {
    welcomeDrafts[agent.id] = {
      content: agent.welcomeMessage || '',
      enabled: agent.welcomeMessageEnabled
    }
  }
  editingWelcomeAgentId.value = editingWelcomeAgentId.value === agent.id ? '' : agent.id
}

async function saveWelcomeMessage(agent: AgentItem) {
  try {
    const draft = welcomeDrafts[agent.id]
    if (!draft) return
    if (draft.enabled && !draft.content.trim()) {
      notice.value = '启用欢迎语时需要填写内容。'
      return
    }
    const updated = await updateAgentWelcomeMessage(agent.id, {
      content: draft.content.trim(),
      enabled: draft.enabled
    })
    const index = agents.value.findIndex(item => item.id === updated.id)
    if (index >= 0) {
      agents.value.splice(index, 1, updated)
    }
    applyWelcomeDrafts(agents.value)
    editingWelcomeAgentId.value = ''
    notice.value = '客服欢迎语已保存。'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '保存欢迎语失败'
  }
}

async function assignConversation(conversationId: string) {
  try {
    const agentUsername = assignDrafts[conversationId]?.trim()
    if (!agentUsername) return
    await assignSupportConversation(conversationId, agentUsername)
    notice.value = 'Conversation assigned.'
    await refreshAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Assignment failed'
  }
}
</script>

<style scoped lang="scss">
.admin-page {
  padding-bottom: 80rpx;
}

.admin-top-nav {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12rpx;
}

.nav-button,
.quick-link {
  margin: 0;
  border: 1rpx solid #d9dde3;
  border-radius: 8rpx;
  background: #ffffff;
  color: #101820;
  box-shadow: none;
}

.nav-button {
  padding: 18rpx 12rpx;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 1.2;
}

.nav-button.active {
  color: #ffffff;
  border-color: #002fa7;
  background: #002fa7;
}

.admin-home-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14rpx;
}

.quick-link {
  padding: 22rpx;
  text-align: left;
}

.quick-title {
  display: block;
  font-size: 27rpx;
  font-weight: 900;
  color: #101820;
}

.quick-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #68727d;
}

.tab-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150rpx, 1fr));
  gap: 12rpx;
  align-items: stretch;
}

.tab-row button {
  width: 100%;
  margin: 0;
  padding: 18rpx 10rpx;
  font-size: 24rpx;
  line-height: 1.2;
}

.active-tab {
  color: #ffffff;
  border-color: #002fa7;
  background: #002fa7;
}

.active-soft {
  color: #0d9b56;
  border-color: #13d66f;
  background: #effff5;
}

.list-row {
  padding: 18rpx 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  border-bottom: 1rpx solid #eef1f3;
}

.agent-list-row {
  align-items: flex-start;
  flex-wrap: wrap;
}

.agent-row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  flex-wrap: wrap;
}

.welcome-editor {
  width: 100%;
  padding: 18rpx;
  border: 1rpx solid #e3e8ef;
  border-radius: 8rpx;
  background: #f8fafc;
}

.welcome-textarea {
  width: 100%;
  min-height: 180rpx;
  padding: 18rpx;
  box-sizing: border-box;
  line-height: 1.45;
}

.welcome-editor-footer {
  margin-top: 12rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
}

.compact-row {
  border-bottom: none;
}

.support-row-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
  flex-wrap: wrap;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  flex-wrap: wrap;
}

.row-title {
  display: block;
  font-size: 29rpx;
  font-weight: 800;
  color: #171717;
}

.row-meta {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #818891;
}

.mini-button {
  min-width: 150rpx;
  padding: 14rpx 18rpx;
  font-size: 23rpx;
}

.conversation-card {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef1f3;
}

.assign-row {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.assign-input {
  height: 74rpx;
  font-size: 25rpx;
}

.growth-user-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10rpx;
  flex-wrap: wrap;
}

.birthday-admin-input {
  width: 210rpx;
  height: 66rpx;
}

.benefit-config-grid,
.holiday-form {
  margin-top: 18rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.holiday-form {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.holiday-enabled {
  min-height: 74rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  color: #68727d;
  font-size: 23rpx;
}

.benefit-save-button {
  width: 100%;
  margin-top: 18rpx;
}

.last-message {
  margin-top: 12rpx;
  padding: 16rpx;
  border-radius: 18rpx;
  background: #f6f8fa;
  color: #5f6872;
  font-size: 23rpx;
  line-height: 1.45;
}

.admin-message-list {
  margin-top: 14rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.admin-message {
  padding: 14rpx 16rpx;
  border-radius: 16rpx;
  background: #f6f8fa;
}

.message-meta {
  display: block;
  font-size: 21rpx;
  color: #87919c;
}

.message-body {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #22272d;
  line-height: 1.45;
  word-break: break-word;
}

.broadcast-type-row {
  margin-top: 14rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.admin-balance-panel {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.balance-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14rpx;
}

.balance-number {
  display: block;
  margin-top: 6rpx;
  font-size: 32rpx;
  font-weight: 900;
  color: #101820;
  line-height: 1.15;
  word-break: break-word;
}

.broadcast-compose {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.broadcast-caption-input {
  width: 100%;
  min-height: 150rpx;
  padding: 18rpx;
  box-sizing: border-box;
  line-height: 1.5;
}

.broadcast-media-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.broadcast-media-preview {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10rpx;
  padding: 14rpx;
  border: 1rpx solid #d9dde3;
  border-radius: 12rpx;
  background: #f7f9fb;
}

.broadcast-preview-image,
.broadcast-record-image {
  width: min(520rpx, 100%);
  height: 300rpx;
  background: #eef1f4;
}

.broadcast-preview-video,
.broadcast-record-video {
  width: min(640rpx, 100%);
  height: 360rpx;
  background: #111111;
}

.broadcast-record-image,
.broadcast-record-video {
  display: block;
  margin: 12rpx 0;
}

.danger-soft {
  color: #b42318;
  border-color: #f0b7b2;
  background: #fff3f2;
}

.admin-order-identity {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
}

.admin-order-logo {
  width: 56rpx;
  height: 56rpx;
  flex: 0 0 auto;
  border: 1rpx solid #d9dde3;
  background: #f7f7f8;
}

.claim-type {
  display: inline-flex;
  margin-bottom: 8rpx;
  padding: 5rpx 9rpx;
  border: 1rpx solid #b9c9ef;
  border-radius: 4rpx;
  background: #f2f6ff;
  color: #002fa7;
  font-size: 20rpx;
  font-weight: 800;
}

.claim-type.physical {
  border-color: #efd389;
  background: #fff8df;
  color: #7a5600;
}

.physical-claim-card {
  border-left: 5rpx solid #e9a900;
}

.address-meta {
  overflow-wrap: anywhere;
}

.growth-summary-panel,
.rule-grid,
.prize-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14rpx;
}

.growth-stat,
.rule-card,
.prize-card {
  min-width: 0;
  padding: 18rpx;
  border-radius: 14rpx;
  background: #f6f8fa;
  border: 1rpx solid #eef1f3;
}

.section-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.reward-setting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 14rpx;
}

.bonus-config-card {
  padding: 18rpx 0;
  border-bottom: 1rpx solid #eef1f3;
}

.bonus-config-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.status-pill.danger,
.danger-action {
  color: #d64242;
  border-color: #f0b1ab;
  background: rgba(244, 91, 91, 0.14);
}

.status-pill.warning {
  color: #b26a00;
  background: rgba(255, 178, 76, 0.18);
}

.notice-text {
  display: block;
  text-align: center;
  font-size: 24rpx;
  color: #5d646d;
}

@media (max-width: 760px) {
  .admin-top-nav,
  .admin-home-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .growth-summary-panel,
  .rule-grid,
  .prize-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bonus-config-grid {
    grid-template-columns: 1fr;
  }

  .benefit-config-grid,
  .holiday-form {
    grid-template-columns: 1fr;
  }

  .growth-user-actions {
    width: 100%;
    justify-content: stretch;
  }

  .birthday-admin-input,
  .growth-user-actions .mini-button {
    width: 100%;
  }

  .balance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
