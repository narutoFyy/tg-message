<template>
  <view class="chat-container">
    <!-- 左侧客户列表 -->
    <view class="customer-sidebar" :class="{ 'sidebar-hidden': isMobile && showChat }">
      <!-- 顶部搜索栏 -->
      <view class="sidebar-header">
        <view v-if="isAdmin" class="admin-sidebar-tools">
          <button class="admin-console-button" @click="goAdminConsole">管理员总控台</button>
          <view class="agent-filter-block">
            <text class="filter-label">按客服筛选</text>
            <picker mode="selector" :range="agentFilterLabels" :value="selectedAgentFilterIndex" @change="onAgentFilterChange">
              <view class="agent-filter-picker">
                <text>{{ selectedAgentFilterLabel }}</text>
                <text class="picker-caret">⌄</text>
              </view>
            </picker>
            <text class="filter-count">当前 {{ filteredSupportConversations.length }} 个会话</text>
          </view>
        </view>
        <view class="search-mode-row">
          <view
            :class="['search-mode-pill', searchMode === 'customers' && 'active']"
            @click="setSearchMode('customers')"
          >
            用户
          </view>
          <view
            :class="['search-mode-pill', searchMode === 'messages' && 'active']"
            @click="setSearchMode('messages')"
          >
            聊天记录
          </view>
        </view>
        <view class="search-box">
          <text class="search-icon">搜</text>
          <input v-model="searchKeyword" class="search-input" :placeholder="searchPlaceholder" />
        </view>
      </view>

      <!-- 客户列表 -->
      <scroll-view scroll-y class="customer-list-scroll">
        <view v-if="isSearching && searchMode === 'messages'" class="search-result-list">
          <view v-if="supportSearchLoading" class="mini-search-state">正在搜索...</view>
          <view
            v-for="item in filteredMessageSearchResults"
            :key="item.messageId"
            class="message-search-item"
            @click="selectMessageSearchResult(item.conversationId)"
          >
            <view class="info-row">
              <text class="customer-name">{{ item.displayName || item.customerUsername }}</text>
              <text class="message-time">{{ formatTime(item.createdAt) }}</text>
            </view>
            <text class="customer-note">{{ countryLabel(item.phoneCountryCode) }} / {{ item.senderRole }}</text>
            <text v-if="isAdmin" class="customer-note">客服：{{ assignedAgentForConversation(item.conversationId) }}</text>
            <text class="last-message search-snippet">{{ item.snippet }}</text>
          </view>
          <view v-if="!supportSearchLoading && filteredMessageSearchResults.length === 0" class="empty-list">
            <text class="empty-text">暂无聊天记录</text>
          </view>
        </view>

        <view v-else-if="isSearching" class="search-result-list">
          <view v-if="supportSearchLoading" class="mini-search-state">正在搜索...</view>
          <view
            v-for="item in filteredCustomerSearchResults"
            :key="item.conversationId"
            :class="['customer-item', { 'active': item.conversationId === activeConversationId }]"
            @click="selectSearchCustomer(item.conversationId)"
          >
            <view class="customer-avatar">
              <view class="avatar-letter">{{ (item.displayName || item.customerUsername || 'U').slice(0, 1).toUpperCase() }}</view>
              <view :class="['online-dot', { offline: !item.online }]"></view>
            </view>
            <view class="customer-info">
              <view class="info-row">
                <text class="customer-name">{{ item.displayName || item.customerUsername }}</text>
                <text class="message-time">{{ formatTime(item.lastMessageTime) }}</text>
              </view>
              <text class="customer-note">{{ countryLabel(item.phoneCountryCode) }} / {{ item.vipLevel }} / {{ item.vipPoints }} pts</text>
              <text v-if="isAdmin" class="customer-note">客服：{{ assignedAgentForConversation(item.conversationId) }}</text>
              <view class="info-row">
                <text class="last-message">{{ item.phone || '@' + item.customerUsername }}</text>
                <view v-if="item.unreadCount > 0" class="unread-badge">{{ item.unreadCount }}</view>
              </view>
            </view>
          </view>
          <view v-if="!supportSearchLoading && filteredCustomerSearchResults.length === 0" class="empty-list">
            <text class="empty-text">没有找到用户</text>
          </view>
        </view>

        <view v-else>
          <view v-for="group in countryGroups" :key="group.code" class="country-group">
            <view class="country-group-head" @click="toggleCountryGroup(group.code)">
              <view>
                <text class="country-title">{{ group.label }}</text>
                <text class="country-subtitle">{{ group.code }}</text>
              </view>
              <view class="country-head-right">
                <text class="country-count">{{ group.conversations.length }}</text>
                <text :class="['country-caret', collapsedCountryGroups[group.code] && 'collapsed']">⌄</text>
              </view>
            </view>
            <view v-if="!collapsedCountryGroups[group.code]">
              <view
                v-for="conv in group.conversations"
                :key="conv.conversationId"
                :class="['customer-item', { 'active': conv.conversationId === activeConversationId }]"
                @click="selectCustomer(conv)"
              >
                <!-- 头像 -->
                <view class="customer-avatar">
                  <image class="avatar-img" :src="customerAvatar(conv)" mode="aspectFill" />
                  <view :class="['online-dot', { offline: !conv.online }]"></view>
                </view>

                <!-- 客户信息 -->
                <view class="customer-info">
                  <view class="info-row">
                    <text class="customer-name">{{ customerDisplayName(conv) }}</text>
                    <text class="message-time">{{ formatTime(conv.lastMessageTime) }}</text>
                  </view>
                  <text v-if="conv.agentNote" class="customer-note">{{ conv.customerUsername }}</text>
                  <text v-else class="customer-note">{{ conv.vipLevel || 'VIP1' }} / {{ conv.vipPoints || '0' }} pts</text>
                  <text v-if="isAdmin" class="customer-note">客服：{{ assignedAgentLabel(conv) }}</text>
                  <view class="info-row">
                    <text class="last-message">{{ getLastMessage(conv) }}</text>
                    <view v-if="displayUnreadCount(conv) > 0" class="unread-badge">{{ displayUnreadCount(conv) }}</view>
                  </view>
                </view>
              </view>
            </view>
          </view>

          <view v-if="countryGroups.length === 0" class="empty-list">
            <text class="empty-text">暂无客户</text>
          </view>
        </view>
      </scroll-view>

      <!-- 底部统计栏 -->
      <view class="sidebar-footer">
        <view class="stat-item">
          <text class="stat-label">客户</text>
          <text class="stat-value">{{ balanceSummary?.userCount || 0 }}</text>
        </view>
        <view class="stat-item">
          <text class="stat-label">余额</text>
          <text class="stat-value">{{ balanceSummary?.availableTotal || '0.00' }}</text>
        </view>
      </view>
    </view>

    <!-- 右侧聊天区域 -->
    <view class="chat-main" :class="{ 'chat-hidden': isMobile && (!showChat || showMobileProfile) }">
      <!-- 聊天顶部导航 -->
      <view class="chat-header">
        <view class="header-left">
          <view v-if="isMobile" class="back-btn" @click="backToList">
            <text>‹</text>
          </view>
          <image class="header-avatar" :src="activeCustomer ? customerAvatar(activeCustomer) : uiIcons.user" mode="aspectFill" />
          <view class="header-info">
            <text class="header-name">{{ activeCustomer ? customerDisplayName(activeCustomer) : '选择客户' }}</text>
            <text v-if="activeCustomer?.agentNote" class="header-note">{{ activeCustomer.customerUsername }}</text>
            <text :class="['header-status', { offline: connectionStatusOffline }]">{{ connectionStatusLabel }}</text>
          </view>
        </view>
        <view class="header-actions">
          <view
            :class="['icon-action', soundEnabled && 'is-on']"
            :title="soundEnabled ? '关闭声音提醒' : '开启声音提醒'"
            @click="toggleSound"
          >
            <text class="icon-bell"></text>
          </view>
          <view class="icon-action primary" title="视频通话" @click="startVideoCall">
            <text class="icon-video"></text>
          </view>
          <view class="header-menu-wrap">
            <view class="icon-action" title="更多操作" @click="toggleMoreMenu">
              <text class="icon-more"></text>
            </view>
            <view v-if="showHeaderMenu" class="header-menu">
              <view v-if="isMobile" class="header-menu-item" @click="handleHeaderMenu('profile')">
                <text class="menu-icon profile"></text>
                <text>客户资料</text>
              </view>
              <view class="header-menu-item" @click="handleHeaderMenu('note')">
                <text class="menu-icon note"></text>
                <text>备注客户</text>
              </view>
              <view class="header-menu-item" @click="handleHeaderMenu('broadcast')">
                <text class="menu-icon broadcast"></text>
                <text>群发消息</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 聊天消息区域 -->
      <scroll-view scroll-y class="message-area" :scroll-into-view="messageScrollTarget" @click="closeChatPanels">
        <view class="message-list">
          <view class="date-divider">
            <text>今天</text>
          </view>

          <view
            v-for="msg in conversation"
            :key="msg.id"
            :class="['message-wrapper', isMine(msg) ? 'mine' : 'theirs']"
          >
            <!-- 对方消息显示头像 -->
            <ChatMessageBubble
              :message="msg"
              :mine="isMine(msg)"
              :avatar-src="activeCustomer ? customerAvatar(activeCustomer) : uiIcons.user"
              :translation="translationFor(msg)"
              :call-title="videoCallTitle(msg)"
              :call-room="videoCallRoom(msg)"
              :call-status="videoCallStatus(msg)"
              :call-status-label="videoCallStatusLabel(msg)"
              :call-caption="videoCallCaption(msg)"
              call-answer-label="接听"
              call-reject-label="拒绝"
              call-enter-label="进入"
              :can-answer-call="canAnswerVideoMessage(msg)"
              :can-reject-call="canRejectVideoMessage(msg)"
              :can-enter-call="canEnterVideoMessage(msg)"
              @preview="previewImage"
              @retry="retryMessage"
              @answer-call="answerVideoMessage"
              @reject-call="rejectVideoMessage"
              @enter-call="enterVideoMessage"
              @message-menu="openMessageMenu"
            />
          </view>

          <view id="msg-bottom"></view>
        </view>
      </scroll-view>

      <!-- 粘贴图片预览 -->
      <ComposerAttachmentPreview
        :attachment="activeAttachment"
        @retry="sendPendingAttachment"
        @clear="clearAttachment"
      />

      <!-- 底部输入区域 -->
      <view :class="['input-area', showComposerTools && isMobile && 'tools-open']">
        <view v-if="replyTarget" class="reply-composer">
          <view class="reply-composer-body">
            <text class="reply-composer-label">引用</text>
            <text class="reply-composer-text">{{ replyTargetText }}</text>
          </view>
          <view class="reply-composer-close" @click="clearReplyTarget">×</view>
        </view>
        <view class="input-row">
          <view class="composer-tools-wrap">
            <view :class="['composer-tool-main', showComposerTools && 'is-open']" title="Attachments" @click="toggleComposerTools">
              <text></text>
              <text></text>
            </view>
            <view v-if="showComposerTools && !isMobile" class="composer-popover">
              <view class="composer-option" @click="chooseComposerTool('image')">
                <text class="composer-option-icon image"></text>
                <text>图片</text>
              </view>
              <view class="composer-option" @click="chooseComposerTool('gif')">
                <text class="composer-option-icon gif">GIF</text>
                <text>GIF</text>
              </view>
            </view>
          </view>
          <input v-model="draft" class="message-input" placeholder="输入消息..." @focus="closeComposerTools" @confirm="handleSend" />
          <view class="send-btn" :class="{ 'active': canSend }" @click="handleSend">
            <text>发送</text>
          </view>
        </view>
        <view v-if="showComposerTools && isMobile" class="composer-panel">
          <view class="composer-panel-grid">
            <view class="composer-panel-item" @click="chooseComposerTool('image')">
              <text class="composer-panel-icon image"></text>
              <text>图片</text>
            </view>
            <view class="composer-panel-item" @click="chooseComposerTool('gif')">
              <text class="composer-panel-icon gif">GIF</text>
              <text>GIF</text>
            </view>
          </view>
        </view>
      </view>

      <view
        v-if="messageContextMenu"
        class="message-menu-mask"
        @click="closeMessageMenu"
        @contextmenu.prevent.stop="closeMessageMenu"
      >
        <view class="message-context-menu" :style="messageMenuStyle" @click.stop @contextmenu.prevent.stop>
          <view class="message-context-item" @click="copyContextMessage">复制</view>
          <view v-if="canQuoteMessage(messageContextMenu.message)" class="message-context-item" @click="quoteContextMessage">引用</view>
          <view v-if="canHideMessage(messageContextMenu.message)" class="message-context-item danger" @click="hideContextMessage">Delete</view>
        </view>
      </view>
    </view>

    <view class="customer-profile-panel" :class="{ 'profile-hidden': isMobile && !showMobileProfile }">
      <view class="profile-header">
        <view v-if="isMobile" class="profile-back-btn" @click="closeMobileProfile">
          <text>‹</text>
        </view>
        <view>
          <text class="profile-eyebrow">{{ workbenchText.customer }}</text>
          <text class="profile-title">{{ profileDisplayName }}</text>
        </view>
        <view class="profile-header-actions">
          <view class="language-toggle" @click="toggleWorkbenchLanguage">{{ workbenchLanguageLabel }}</view>
          <view v-if="profile?.customer.status" class="profile-status">{{ statusText(profile.customer.status) }}</view>
        </view>
      </view>

      <view v-if="store.state.supportCustomerProfileLoading" class="profile-empty">
        <text>{{ workbenchText.loading }}</text>
      </view>

      <scroll-view v-else-if="profile" scroll-y class="profile-scroll">
        <view class="profile-section ledger-section">
          <view class="section-head">
            <text class="section-title">{{ ledgerTitleText }}</text>
            <text class="section-count">{{ supportLedger?.summary.userCount || 0 }}</text>
          </view>
          <view class="ledger-summary-grid">
            <view class="ledger-summary-item">
              <text class="metric-label">{{ workbenchText.available }}</text>
              <text class="metric-value">{{ supportLedger?.summary.availableTotal || '0.00' }}</text>
            </view>
            <view class="ledger-summary-item">
              <text class="metric-label">{{ workbenchText.pendingBalance }}</text>
              <text class="metric-value">{{ supportLedger?.summary.pendingTotal || '0.00' }}</text>
            </view>
            <view class="ledger-summary-item">
              <text class="metric-label">{{ workbenchText.withdrawn }}</text>
              <text class="metric-value">{{ supportLedger?.summary.withdrawnTotal || '0.00' }}</text>
            </view>
          </view>
          <view v-if="store.state.supportLedgerLoading" class="mini-empty">{{ ledgerLoadingText }}</view>
          <view v-else-if="ledgerCustomers.length === 0" class="mini-empty">{{ ledgerEmptyText }}</view>
          <view
            v-for="customer in ledgerCustomers"
            :key="customer.conversationId"
            :class="['ledger-row', { active: customer.conversationId === activeConversationId }]"
            @click="selectLedgerCustomer(customer.conversationId)"
          >
            <view class="ledger-row-main">
              <text class="ledger-name">{{ customer.displayName }}</text>
              <text class="ledger-meta">{{ customer.pendingOrderCount }}/{{ customer.orderCount }} orders</text>
            </view>
            <view class="ledger-row-money">
              <text class="ledger-money">{{ customer.availableTotal }}</text>
              <text class="ledger-pending">{{ customer.pendingTotal }}</text>
            </view>
          </view>
        </view>

        <view class="profile-section">
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.username }}</text>
            <text class="profile-value">@{{ profile.customer.username }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.agent }}</text>
            <text class="profile-value">{{ profile.customer.assignedAgent || '-' }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.phone }}</text>
            <text class="profile-value">{{ profile.customer.phone || '-' }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">Country code</text>
            <text class="profile-value">{{ profile.customer.phoneCountryCode || '-' }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.email }}</text>
            <text class="profile-value">{{ profile.customer.email || '-' }}</text>
          </view>
          <view class="profile-row">
            <text class="profile-label">{{ workbenchText.joined }}</text>
            <text class="profile-value">{{ profile.customer.createdAt }}</text>
          </view>
        </view>

        <view class="profile-section" v-if="profile.registrationBonus">
          <view class="section-head">
            <text class="section-title">Registration bonus</text>
            <text :class="['work-status', profile.registrationBonus.status]">{{ profile.registrationBonus.status }}</text>
          </view>
          <text class="work-line strong">{{ profile.registrationBonus.bonusAmount }} {{ profile.registrationBonus.currencyCode }}</text>
          <text class="work-line">{{ profile.registrationBonus.countryCode || '-' }} / {{ profile.registrationBonus.reason }}</text>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">Bank risk</text>
            <text class="section-count">{{ profile.bankAccountRiskMatches.length }}</text>
          </view>
          <view v-if="profile.bankAccountRiskMatches.length === 0" class="mini-empty">No duplicate bank account found.</view>
          <view v-for="risk in profile.bankAccountRiskMatches.slice(0, 4)" :key="`${risk.username}-${risk.accountNumber}-${risk.submittedAt}`" class="work-item risk-item">
            <view class="work-top">
              <text class="work-title">{{ risk.username }}</text>
              <text :class="['work-status', risk.riskLevel]">{{ risk.riskLevel }}</text>
            </view>
            <text class="work-line">{{ risk.reason }}</text>
            <text class="work-line">{{ risk.bankName }} / {{ risk.accountName }} / {{ risk.accountNumber }}</text>
            <text class="work-line">{{ risk.phoneCountryCode || '-' }} / {{ risk.assignedAgent || '-' }}</text>
          </view>
        </view>

        <view class="profile-section metric-grid">
          <view class="metric-item">
            <text class="metric-label">{{ workbenchText.available }}</text>
            <text class="metric-value">{{ profile.balance.availableTotal }}</text>
          </view>
          <view class="metric-item">
            <text class="metric-label">{{ workbenchText.pendingBalance }}</text>
            <text class="metric-value">{{ profile.balance.pendingTotal }}</text>
          </view>
          <view class="metric-item">
            <text class="metric-label">{{ workbenchText.withdrawn }}</text>
            <text class="metric-value">{{ profile.balance.withdrawnTotal }}</text>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">{{ workbenchText.orders }}</text>
            <text class="section-count">{{ profile.orders.length }}</text>
          </view>
          <view v-if="profile.orders.length === 0" class="mini-empty">{{ workbenchText.noOrders }}</view>
          <view v-else class="order-focus">
            <picker mode="selector" :range="orderPickerOptions" :value="selectedOrderIndex" @change="handleOrderPick">
              <view class="order-picker">
                <text class="order-picker-label">{{ orderSelectorText }}</text>
                <text class="order-picker-value">{{ selectedOrder?.orderNo }}</text>
                <text class="order-picker-arrow">⌄</text>
              </view>
            </picker>
          </view>
          <view v-if="selectedOrder" class="work-item selected-work-item">
            <view class="work-top">
              <text class="work-title">{{ selectedOrder.orderNo }}</text>
              <text :class="['work-status', selectedOrder.status]">{{ statusText(selectedOrder.status) }}</text>
            </view>
            <view class="work-card-line">
              <image class="work-card-logo" :src="cardLogoFor(selectedOrder.cardName)" mode="aspectFit" />
              <text class="work-line">{{ selectedOrder.cardName }} / {{ selectedOrder.faceValue }}</text>
            </view>
            <text class="work-line strong">{{ selectedOrder.payoutAmount }}</text>
            <view class="work-actions">
              <button v-if="selectedOrder.status === 'pending'" class="mini-btn" @click="changeOrderStatus(selectedOrder.id, 'processing')">{{ workbenchText.process }}</button>
              <button v-if="selectedOrder.status === 'pending' || selectedOrder.status === 'processing'" class="mini-btn primary" @click="changeOrderStatus(selectedOrder.id, 'completed')">{{ workbenchText.complete }}</button>
              <button v-if="selectedOrder.status === 'pending' || selectedOrder.status === 'processing'" class="mini-btn danger" @click="changeOrderStatus(selectedOrder.id, 'disputed')">{{ workbenchText.dispute }}</button>
              <button v-if="selectedOrder.status === 'pending' || selectedOrder.status === 'processing'" class="mini-btn danger" @click="cancelSelectedOrder">{{ workbenchText.cancelOrder }}</button>
            </view>
            <text v-if="selectedOrder.status === 'canceled'" class="work-line">{{ selectedOrder.cancelReason }} {{ selectedOrder.cancelNote }}</text>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">{{ workbenchText.withdrawals }}</text>
            <text class="section-count">{{ profile.withdrawals.length }}</text>
          </view>
          <view v-if="profile.withdrawals.length === 0" class="mini-empty">{{ workbenchText.noWithdrawals }}</view>
          <view v-for="item in profile.withdrawals.slice(0, 4)" :key="item.id" class="work-item">
            <view class="work-top">
              <text class="work-title">{{ item.requestNo }}</text>
              <text :class="['work-status', item.status]">{{ statusText(item.status) }}</text>
            </view>
            <text class="claim-kind">{{ item.sourceType === 'lottery_cash' ? 'Lottery cash claim' : 'Wallet withdrawal' }}</text>
            <text v-if="item.sourceType === 'lottery_cash'" class="work-line strong">{{ item.prizeName }} / {{ item.lotteryRecordId }}</text>
            <text class="work-line">{{ item.amount }} / {{ item.country }}</text>
            <text class="work-line">{{ item.accountName }} / {{ item.bankName }} / {{ item.accountNumber }}</text>
            <view v-if="item.status === 'pending'" class="work-actions">
              <button class="mini-btn primary" @click="changeWithdrawalStatus(item.id, 'completed')">{{ workbenchText.markPaid }}</button>
            </view>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">Physical prize claims</text>
            <text class="section-count">{{ profile.lotteryFulfillments.length }}</text>
          </view>
          <view v-if="profile.lotteryFulfillments.length === 0" class="mini-empty">No physical prize claims</view>
          <view v-for="item in profile.lotteryFulfillments.slice(0, 4)" :key="item.id" class="work-item physical-claim">
            <view class="work-top">
              <text class="work-title">{{ item.orderNo }}</text>
              <text :class="['work-status', item.status]">{{ statusText(item.status) }}</text>
            </view>
            <text class="claim-kind">Physical prize delivery</text>
            <text class="work-line strong">{{ item.prizeName }} / {{ item.lotteryRecordId }}</text>
            <text class="work-line">{{ item.recipientName }} / {{ item.phone }} / {{ item.country }}</text>
            <text class="work-line address-line">{{ item.addressLine }}</text>
            <view v-if="item.status === 'pending'" class="work-actions">
              <button class="mini-btn primary" @click="changeLotteryFulfillmentStatus(item.id, 'completed')">Mark delivered</button>
            </view>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">{{ workbenchText.loans }}</text>
            <text class="section-count">{{ profile.loans.length }}</text>
          </view>
          <view v-if="profile.loans.length === 0" class="mini-empty">{{ workbenchText.noLoans }}</view>
          <view v-for="loan in profile.loans.slice(0, 4)" :key="loan.id" class="work-item">
            <view class="work-top">
              <text class="work-title">{{ loan.applicationNo }}</text>
              <text :class="['work-status', loan.status]">{{ statusText(loan.status) }}</text>
            </view>
            <text class="work-line">{{ loan.amount }} / {{ loan.country }}</text>
            <text class="work-line">{{ loan.purpose }}</text>
          </view>
        </view>

        <view class="profile-section">
          <view class="section-head">
            <text class="section-title">{{ workbenchText.calls }}</text>
            <text class="section-count">{{ profile.videoSessions.length }}</text>
          </view>
          <view v-if="profile.videoSessions.length === 0" class="mini-empty">{{ workbenchText.noCalls }}</view>
          <view v-for="call in profile.videoSessions.slice(0, 3)" :key="call.id" class="work-item compact">
            <view class="work-top">
              <text class="work-title">{{ call.roomId }}</text>
              <text :class="['work-status', call.status]">{{ statusText(call.status) }}</text>
            </view>
            <text class="work-line">{{ call.createdAt }}</text>
          </view>
        </view>
      </scroll-view>

      <view v-else class="profile-empty">
        <text>{{ workbenchText.selectCustomer }}</text>
      </view>
    </view>

    <view v-if="showBroadcastPanel" class="broadcast-mask" @click="closeBroadcastPanel">
      <view class="broadcast-dialog" @click.stop>
        <view class="broadcast-head">
          <text class="broadcast-title">群发消息</text>
          <text class="broadcast-close" @click="closeBroadcastPanel">×</text>
        </view>
        <textarea
          v-model="broadcastDraft"
          class="broadcast-textarea"
          :placeholder="broadcastMediaUrl ? '可选：输入图片或视频说明文字' : '输入群发内容'"
        />
        <view class="broadcast-media-actions">
          <button class="mini-btn" :disabled="broadcastUploading" @click="chooseSupportBroadcastImage">选择图片</button>
          <button class="mini-btn" :disabled="broadcastUploading" @click="chooseSupportBroadcastVideo">选择视频</button>
          <button v-if="broadcastMediaUrl" class="mini-btn danger" @click="clearSupportBroadcastMedia">移除附件</button>
          <text v-if="broadcastUploading" class="broadcast-uploading">正在上传...</text>
        </view>
        <view v-if="broadcastMediaUrl" class="broadcast-media-preview">
          <image
            v-if="broadcastMediaType === 'image'"
            class="broadcast-preview-image"
            :src="resolveMediaUrl(broadcastMediaUrl)"
            mode="aspectFit"
          />
          <video
            v-else
            class="broadcast-preview-video"
            :src="resolveMediaUrl(broadcastMediaUrl)"
            controls
          />
          <text class="broadcast-preview-name">{{ broadcastMediaName || (broadcastMediaType === 'image' ? '图片' : '视频') }}</text>
        </view>
        <input v-model="broadcastKeyword" class="broadcast-input" placeholder="搜索备注、用户名、手机号、银行账户" />
        <view class="country-filter-row">
          <button
            v-for="code in broadcastCountryOptions"
            :key="code"
            :class="['filter-chip', broadcastCountryCodes.includes(code) && 'active']"
            @click="toggleBroadcastCountry(code)"
          >
            {{ code }}
          </button>
        </view>
        <view class="broadcast-tools">
          <button class="mini-btn" @click="selectAllBroadcastResults">全选搜索结果</button>
          <button class="mini-btn" @click="clearBroadcastSelection">清空选择</button>
          <text class="broadcast-count">已选 {{ broadcastSelectedConversationIds.length }} / 命中 {{ broadcastFilteredConversations.length }}</text>
        </view>
        <scroll-view scroll-y class="broadcast-target-list">
          <view
            v-for="conv in broadcastFilteredConversations"
            :key="conv.conversationId"
            :class="['broadcast-target', broadcastSelectedConversationIds.includes(conv.conversationId) && 'active']"
            @click="toggleBroadcastCustomer(conv.conversationId)"
          >
            <text class="broadcast-target-name">{{ customerDisplayName(conv) }}</text>
            <text class="broadcast-target-meta">{{ conv.customerUsername }} {{ conv.agentNote }}</text>
          </view>
          <view v-if="broadcastFilteredConversations.length === 0" class="mini-empty">没有匹配客户</view>
        </scroll-view>
        <view class="broadcast-actions">
          <button class="mini-btn" @click="closeBroadcastPanel">取消</button>
          <button class="mini-btn primary" :disabled="broadcastUploading" @click="submitFilteredBroadcast">发送</button>
        </view>
      </view>
    </view>

    <view v-if="incomingVideoInvite" class="incoming-call-mask">
      <view class="incoming-call-dialog">
        <text class="incoming-call-title">Video call</text>
        <text class="incoming-call-copy">{{ incomingVideoInvite.initiatorUsername }} is calling you.</text>
        <view class="incoming-call-actions">
          <button class="incoming-call-btn decline" @click="declineIncomingVideo">Decline</button>
          <button class="incoming-call-btn answer" @click="answerIncomingVideo">Answer</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import ChatMessageBubble from '@/components/chat/ChatMessageBubble.vue'
import ComposerAttachmentPreview from '@/components/chat/ComposerAttachmentPreview.vue'
import { useComposerAttachments, type ComposerAttachmentKind } from '@/components/chat/useComposerAttachments'
import { fetchAgents, translateToChinese, uploadImage, uploadVideo } from '@/utils/api'
import { connectChatSocket } from '@/utils/realtime'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import { cardLogoFor, uiIcons } from '@/utils/art'
import type { AgentItem, ChatMessage, PresenceEvent, SupportConversationItem, VideoCallMessagePayload, VideoInviteEvent, VideoSessionItem, VideoSessionStatusEvent } from '@/types'
import type { ChatRealtimePayload, ChatReadReceiptEvent } from '@/types'
import type { LotteryFulfillmentItem, TransactionItem, WithdrawalItem } from '@/types'

const store = useAppStore()
const draft = ref('')
const searchKeyword = ref('')
const searchMode = ref<'customers' | 'messages'>('customers')
const supportSearchLoading = ref(false)
const supportAgents = ref<AgentItem[]>([])
const selectedAgentFilter = ref('all')
const SUPPORT_GROUP_COLLAPSED_KEY = 'support-country-groups-collapsed'
const collapsedCountryGroups = ref<Record<string, boolean>>({})
let supportSearchTimer: ReturnType<typeof setTimeout> | null = null
let supportSearchSerial = 0
const showBroadcastPanel = ref(false)
const broadcastDraft = ref('')
const broadcastKeyword = ref('')
const broadcastMediaUrl = ref('')
const broadcastMediaName = ref('')
const broadcastMediaType = ref<'image' | 'video'>('image')
const broadcastUploading = ref(false)
const broadcastCountryOptions = ['+234', '+91', '+237', '+233', '+254']
const broadcastCountryCodes = ref<string[]>([])
const broadcastSelectedConversationIds = ref<string[]>([])
const socketTask = ref<UniApp.SocketTask | null>(null)
const socketStatus = ref<'connecting' | 'online' | 'offline'>('connecting')
const presenceRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const readRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null)
const {
  activeAttachment,
  hasAttachment,
  isUploading: isAttachmentUploading,
  addFile,
  addPath,
  clearAttachment,
  setStatus
} = useComposerAttachments()
const messageScrollTarget = ref('msg-bottom')
const SOUND_ENABLED_KEY = 'support-chat-sound-enabled'
const WORKBENCH_LANGUAGE_KEY = 'support-workbench-language'
const audioEnabled = ref(uni.getStorageSync(SOUND_ENABLED_KEY) !== false)
const audioUnlocked = ref(false)
const workbenchLanguage = ref<'zh' | 'en'>((uni.getStorageSync(WORKBENCH_LANGUAGE_KEY) || 'zh') as 'zh' | 'en')
const showChat = ref(false)
const isMobile = ref(false)
const showMobileProfile = ref(false)
const activeConversationId = ref('')
const showHeaderMenu = ref(false)
const showComposerTools = ref(false)
const handledVideoInvites = new Set<string>()
const localVideoStatuses = ref<Record<string, VideoSessionItem['status']>>({})
const incomingVideoInvite = ref<VideoInviteEvent | null>(null)
const translations = reactive<Record<string, string>>({})
const translatingIds = new Set<string>()
const pendingRouteConversationId = ref('')
const replyTarget = ref<ChatMessage['replyTo'] | null>(null)
const messageContextMenu = ref<{ message: ChatMessage; x: number; y: number } | null>(null)
const lastContextMenuPoint = ref<{ clientX: number; clientY: number; time: number } | null>(null)

const conversation = computed(() => store.state.supportMessages)
const isAgent = computed(() => store.state.currentUser?.roleCode === 'AGENT' || store.state.currentUser?.roleCode === 'ADMIN')
const isAdmin = computed(() => store.state.currentUser?.roleCode === 'ADMIN')
const balanceSummary = computed(() => store.state.balanceSummary)
const canSend = computed(() => draft.value.trim().length > 0 || hasAttachment.value)
const replyTargetText = computed(() => previewMessageContent(replyTarget.value?.content || ''))
const messageMenuStyle = computed(() => {
  const menu = messageContextMenu.value
  if (!menu) return ''
  return `left:${menu.x}px;top:${menu.y}px;`
})

const trimmedSearchKeyword = computed(() => searchKeyword.value.trim())
const isSearching = computed(() => trimmedSearchKeyword.value.length > 0)
const searchPlaceholder = computed(() => searchMode.value === 'messages' ? '搜索聊天记录' : '搜索用户')
const agentFilterOptions = computed(() => [
  { label: '全部客服', value: 'all' },
  { label: '未分配客户', value: 'unassigned' },
  ...supportAgents.value.map(agent => ({
    label: `${agent.username}${agent.status === 'ACTIVE' ? '' : '（停用）'}`,
    value: agent.username
  }))
])
const agentFilterLabels = computed(() => agentFilterOptions.value.map(option => option.label))
const selectedAgentFilterIndex = computed(() => {
  const index = agentFilterOptions.value.findIndex(option => option.value === selectedAgentFilter.value)
  return index >= 0 ? index : 0
})
const selectedAgentFilterLabel = computed(() => agentFilterOptions.value[selectedAgentFilterIndex.value]?.label || '全部客服')
const filteredSupportConversations = computed(() =>
  store.state.supportConversations.filter(conversationMatchesAgentFilter)
)
const sortedConversations = computed(() =>
  [...filteredSupportConversations.value].sort((left, right) => {
    const unreadDiff = displayUnreadCount(right) - displayUnreadCount(left)
    if (unreadDiff !== 0) return unreadDiff
    return conversationTimestamp(right) - conversationTimestamp(left)
  })
)
const filteredCustomerSearchResults = computed(() =>
  store.state.supportCustomerSearchResults.filter(item => conversationIdMatchesAgentFilter(item.conversationId))
)
const filteredMessageSearchResults = computed(() =>
  store.state.supportMessageSearchResults.filter(item => conversationIdMatchesAgentFilter(item.conversationId))
)
const countryGroups = computed(() => {
  const groups = new Map<string, SupportConversationItem[]>()
  sortedConversations.value.forEach((conv) => {
    const code = normalizeCountryCode(conv.phoneCountryCode)
    groups.set(code, [...(groups.get(code) || []), conv])
  })
  return Array.from(groups.entries())
    .map(([code, conversations]) => ({
      code,
      label: countryLabel(code),
      conversations
    }))
    .sort((left, right) => {
      if (left.code === 'Unknown') return 1
      if (right.code === 'Unknown') return -1
      return left.label.localeCompare(right.label)
    })
})

const broadcastFilteredConversations = computed(() => {
  const keyword = broadcastKeyword.value.trim().toLowerCase()
  const countryCodes = broadcastCountryCodes.value
  return store.state.supportConversations.filter((conv) => {
    const matchesKeyword = !keyword || conversationMatchesKeyword(conv, keyword)
    const profile = store.state.supportCustomerProfileCache[conv.conversationId]
    const profileCountry = profile?.customer.phoneCountryCode || ''
    const matchesCountry = countryCodes.length === 0 || !profileCountry || countryCodes.includes(profileCountry)
    return matchesKeyword && matchesCountry
  })
})

const activeCustomer = computed(() =>
  store.state.supportConversations.find(c => c.conversationId === activeConversationId.value)
)
const connectionStatusLabel = computed(() => {
  if (socketStatus.value === 'connecting') return '连接中'
  if (socketStatus.value === 'offline') return '重连中'
  return activeCustomer.value?.online ? '在线' : '离线'
})
const connectionStatusOffline = computed(() => socketStatus.value !== 'online' || !activeCustomer.value?.online)
const soundEnabled = computed(() => audioEnabled.value)
const profile = computed(() => store.state.activeSupportCustomerProfile)
const supportLedger = computed(() => store.state.supportLedger)
const ledgerCustomers = computed(() => supportLedger.value?.customers.slice(0, 6) || [])
const selectedOrderId = ref('')
const selectedOrder = computed(() => {
  const orders = profile.value?.orders || []
  return orders.find(order => order.id === selectedOrderId.value) || orders[0] || null
})
const selectedOrderIndex = computed(() => {
  const orders = profile.value?.orders || []
  const index = orders.findIndex(order => order.id === selectedOrder.value?.id)
  return index >= 0 ? index : 0
})
const orderPickerOptions = computed(() =>
  (profile.value?.orders || []).map(order => `${order.orderNo} - ${statusText(order.status)}`)
)
const orderSelectorText = computed(() => workbenchLanguage.value === 'zh' ? '选择订单' : 'Select order')
const ledgerTitleText = computed(() => workbenchLanguage.value === 'zh' ? '客户账务' : 'Ledger')
const ledgerLoadingText = computed(() => workbenchLanguage.value === 'zh' ? '正在加载账务...' : 'Loading ledger...')
const ledgerEmptyText = computed(() => workbenchLanguage.value === 'zh' ? '暂无账务数据' : 'No ledger data')
const workbenchCopy = {
  zh: {
    customer: '客户',
    username: '用户名',
    agent: '客服',
    phone: '电话',
    email: '邮箱',
    joined: '加入时间',
    available: '可用余额',
    pendingBalance: '待结算',
    withdrawn: '已提现',
    orders: '订单',
    withdrawals: '提现',
    loans: '贷款',
    calls: '通话',
    noOrders: '暂无订单',
    noWithdrawals: '暂无提现',
    noLoans: '暂无贷款',
    noCalls: '暂无通话',
    process: '处理中',
    complete: '完成',
    dispute: '争议',
    cancelOrder: '取消订单',
    markPaid: '标记已付款',
    loading: '正在加载客户资料...',
    selectCustomer: '选择客户后查看订单、余额、提现和贷款。',
    orderUpdated: '订单已更新',
    orderUpdateFailed: '订单更新失败',
    orderCanceled: '订单已取消',
    orderCancelFailed: '订单取消失败',
    withdrawalUpdated: '提现已更新',
    withdrawalUpdateFailed: '提现更新失败',
    noCustomer: '未选择客户',
    switchTo: 'English'
  },
  en: {
    customer: 'Customer',
    username: 'Username',
    agent: 'Agent',
    phone: 'Phone',
    email: 'Email',
    joined: 'Joined',
    available: 'Available',
    pendingBalance: 'Pending',
    withdrawn: 'Withdrawn',
    orders: 'Orders',
    withdrawals: 'Withdrawals',
    loans: 'Loans',
    calls: 'Calls',
    noOrders: 'No orders',
    noWithdrawals: 'No withdrawals',
    noLoans: 'No loans',
    noCalls: 'No calls',
    process: 'Process',
    complete: 'Complete',
    dispute: 'Dispute',
    cancelOrder: 'Cancel order',
    markPaid: 'Mark paid',
    loading: 'Loading customer details...',
    selectCustomer: 'Select a customer to view orders, balance, withdrawals, and loans.',
    orderUpdated: 'Order updated',
    orderUpdateFailed: 'Order update failed',
    orderCanceled: 'Order canceled',
    orderCancelFailed: 'Order cancellation failed',
    withdrawalUpdated: 'Withdrawal updated',
    withdrawalUpdateFailed: 'Withdrawal update failed',
    noCustomer: 'No customer selected',
    switchTo: '中文'
  }
}
const statusCopy: Record<string, { zh: string; en: string }> = {
  active: { zh: '正常', en: 'Active' },
  pending: { zh: '待处理', en: 'Pending' },
  processing: { zh: '处理中', en: 'Processing' },
  completed: { zh: '已完成', en: 'Completed' },
  disputed: { zh: '争议', en: 'Disputed' },
  canceled: { zh: '已取消', en: 'Canceled' },
  approved: { zh: '已通过', en: 'Approved' },
  rejected: { zh: '已拒绝', en: 'Rejected' },
  joining: { zh: '接入中', en: 'Joining' },
  ended: { zh: '已结束', en: 'Ended' },
  missed: { zh: '未接通', en: 'Missed' },
  high: { zh: '高风险', en: 'High' },
  medium: { zh: '中风险', en: 'Medium' },
  low: { zh: '低风险', en: 'Low' }
}
const workbenchText = computed(() => workbenchCopy[workbenchLanguage.value])
const workbenchLanguageLabel = computed(() => workbenchText.value.switchTo)
const profileDisplayName = computed(() => {
  if (profile.value) {
    return profile.value.customer.agentNote || profile.value.customer.username
  }
  return activeCustomer.value ? customerDisplayName(activeCustomer.value) : workbenchText.value.noCustomer
})

onLoad((query) => {
  pendingRouteConversationId.value = typeof query?.conversationId === 'string' ? query.conversationId : ''
})

onShow(() => {
  checkMobile()
  store.bootstrap().then(() => {
    if (store.state.currentUser?.roleCode !== 'AGENT' && store.state.currentUser?.roleCode !== 'ADMIN') {
      uni.redirectTo({ url: '/pages/support/index' })
      return
    }
    loadAdminAgentFilters()
    applyPendingSupportDraft()
    if (store.state.supportConversations.length > 0) {
      const routeConversation = store.state.supportConversations.find(item => item.conversationId === pendingRouteConversationId.value)
      activeConversationId.value = routeConversation?.conversationId || store.state.supportConversations[0].conversationId
      pendingRouteConversationId.value = ''
      store.setActiveSupportConversation(activeConversationId.value)
      store.refreshSupportCustomerProfile(activeConversationId.value).catch(() => {})
      connectSocket()
      // 页面加载时标记第一个客户的消息为已读
      store.markSupportRead().catch(() => {})
    }
  })
})

onMounted(() => {
  loadCollapsedCountryGroups()
  attachPasteListener()
  attachContextMenuPointListener()
  startPresenceRefresh()
  window.addEventListener('resize', checkMobile)
})

watch(
  () => conversation.value.map((message) => `${message.id}:${message.content}`).join('|'),
  () => {
    translateVisibleIncomingMessages()
  }
)

watch(
  () => conversation.value.length,
  () => {
    scrollMessagesToBottom()
  }
)

watch(
  () => profile.value?.orders.map(order => order.id).join('|') || '',
  () => {
    const orders = profile.value?.orders || []
    if (orders.length === 0) {
      selectedOrderId.value = ''
      return
    }
    if (!orders.some(order => order.id === selectedOrderId.value)) {
      selectedOrderId.value = orders[0].id
    }
  },
  { immediate: true }
)

watch(
  () => [trimmedSearchKeyword.value, searchMode.value] as const,
  ([keyword, mode]) => {
    scheduleSupportSearch(keyword, mode)
  }
)

onUnmounted(() => {
  if (supportSearchTimer) {
    clearTimeout(supportSearchTimer)
    supportSearchTimer = null
  }
  stopPresenceRefresh()
  stopReadRefresh()
  closeSocket()
  detachPasteListener()
  detachContextMenuPointListener()
  window.removeEventListener('resize', checkMobile)
})

function checkMobile() {
  // #ifdef H5
  isMobile.value = window.innerWidth < 768
  // #endif
}

function setSearchMode(mode: 'customers' | 'messages') {
  searchMode.value = mode
}

function onAgentFilterChange(event: Event) {
  const index = Number((event as unknown as { detail?: { value?: number | string } }).detail?.value || 0)
  selectedAgentFilter.value = agentFilterOptions.value[index]?.value || 'all'
  nextTick(() => {
    ensureActiveConversationVisible()
  })
}

async function loadAdminAgentFilters() {
  if (!isAdmin.value) return
  try {
    supportAgents.value = await fetchAgents()
  } catch (error) {
    console.warn('Failed to load admin agent filters', error)
  }
}

function conversationMatchesAgentFilter(conv: SupportConversationItem) {
  if (!isAdmin.value || selectedAgentFilter.value === 'all') return true
  if (selectedAgentFilter.value === 'unassigned') {
    return !conv.assignedAgent || conv.assignmentStatus?.toLowerCase() === 'unassigned'
  }
  return conv.assignedAgent === selectedAgentFilter.value
}

function conversationIdMatchesAgentFilter(conversationId: string) {
  const conv = store.state.supportConversations.find(item => item.conversationId === conversationId)
  return conv ? conversationMatchesAgentFilter(conv) : selectedAgentFilter.value === 'all'
}

function assignedAgentLabel(conv: SupportConversationItem) {
  return conv.assignedAgent || '未分配'
}

function assignedAgentForConversation(conversationId: string) {
  const conv = store.state.supportConversations.find(item => item.conversationId === conversationId)
  return conv ? assignedAgentLabel(conv) : '未知'
}

function ensureActiveConversationVisible() {
  if (!activeConversationId.value) return
  const current = filteredSupportConversations.value.find(item => item.conversationId === activeConversationId.value)
  if (current) return
  const nextConversation = sortedConversations.value[0]
  if (!nextConversation) {
    activeConversationId.value = ''
    store.setActiveSupportConversation('')
    return
  }
  selectCustomer(nextConversation).catch(() => {})
}

function normalizeCountryCode(code?: string) {
  return (code || '').trim() || 'Unknown'
}

function countryLabel(code?: string) {
  const normalized = normalizeCountryCode(code)
  return {
    '+234': 'Nigeria',
    '+91': 'India',
    '+237': 'Cameroon',
    '+233': 'Ghana',
    '+254': 'Kenya',
    '+86': 'China',
    '+44': 'United Kingdom',
    '+1': 'United States',
    Unknown: 'Unknown country'
  }[normalized] || normalized
}

function conversationTimestamp(conv: SupportConversationItem) {
  const time = Date.parse(conv.lastMessageTime || '')
  return Number.isFinite(time) ? time : 0
}

function loadCollapsedCountryGroups() {
  const cached = uni.getStorageSync(SUPPORT_GROUP_COLLAPSED_KEY)
  if (cached && typeof cached === 'object') {
    collapsedCountryGroups.value = cached as Record<string, boolean>
  }
}

function saveCollapsedCountryGroups() {
  uni.setStorageSync(SUPPORT_GROUP_COLLAPSED_KEY, collapsedCountryGroups.value)
}

function toggleCountryGroup(code: string) {
  collapsedCountryGroups.value = {
    ...collapsedCountryGroups.value,
    [code]: !collapsedCountryGroups.value[code]
  }
  saveCollapsedCountryGroups()
}

function scheduleSupportSearch(keyword: string, mode: 'customers' | 'messages') {
  if (supportSearchTimer) {
    clearTimeout(supportSearchTimer)
    supportSearchTimer = null
  }
  if (!keyword) {
    supportSearchLoading.value = false
    store.clearSupportSearchResults()
    return
  }
  supportSearchLoading.value = true
  supportSearchTimer = setTimeout(() => {
    runSupportSearch(keyword, mode)
  }, 260)
}

async function runSupportSearch(keyword: string, mode: 'customers' | 'messages') {
  const serial = ++supportSearchSerial
  try {
    if (mode === 'messages') {
      await store.searchSupportMessages(keyword)
    } else {
      await store.searchSupportCustomers(keyword)
    }
  } catch (error) {
    if (serial === supportSearchSerial) {
      uni.showToast({ title: error instanceof Error ? error.message : 'Search failed', icon: 'none' })
    }
  } finally {
    if (serial === supportSearchSerial) {
      supportSearchLoading.value = false
    }
  }
}

function previewMessageContent(content: string) {
  const normalized = (content || '').trim().replace(/\s+/g, ' ')
  if (!normalized) return ''
  return normalized.length > 80 ? `${normalized.slice(0, 80)}...` : normalized
}

function conversationMatchesKeyword(conv: SupportConversationItem, keyword: string) {
  const profile = store.state.supportCustomerProfileCache[conv.conversationId]
  const searchable = [
    conv.customerUsername,
    conv.agentNote,
    profile?.customer.phone,
    profile?.customer.phoneCountryCode,
    ...(profile?.withdrawals || []).flatMap(item => [item.accountName, item.bankName, item.accountNumber])
  ]
  return searchable.some(value => (value || '').toLowerCase().includes(keyword))
}

function copyableMessageContent(message: ChatMessage) {
  if (message.type === 'image') return '[图片]'
  if (message.type === 'gif') return '[GIF]'
  if (message.type === 'voice') return '[语音]'
  if (message.type === 'video') return isVideoFileMessage(message) ? '[视频]' : '[视频通话]'
  return message.content || ''
}

function clipboardMessageContent(message: ChatMessage) {
  return message.type === 'text' ? message.content || '' : copyableMessageContent(message)
}

function closeMessageMenu() {
  messageContextMenu.value = null
}

function clearReplyTarget() {
  replyTarget.value = null
}

function canQuoteMessage(message: ChatMessage) {
  return message.author !== 'system' && !message.id.startsWith('local-')
}

function canHideMessage(message: ChatMessage) {
  return message.author !== 'system' && !message.id.startsWith('local-')
}

function rememberContextMenuPoint(event: MouseEvent) {
  lastContextMenuPoint.value = {
    clientX: event.clientX,
    clientY: event.clientY,
    time: Date.now()
  }
}

function attachContextMenuPointListener() {
  // #ifdef H5
  window.addEventListener('contextmenu', rememberContextMenuPoint, true)
  // #endif
}

function detachContextMenuPointListener() {
  // #ifdef H5
  window.removeEventListener('contextmenu', rememberContextMenuPoint, true)
  // #endif
}

function resolveContextMenuPoint(point?: { clientX: number; clientY: number }) {
  const recent = lastContextMenuPoint.value
  if (recent && Date.now() - recent.time < 250) {
    return recent
  }
  return point
}

function openMessageMenu(message: ChatMessage, point?: { clientX: number; clientY: number }) {
  showHeaderMenu.value = false
  showComposerTools.value = false
  let x = 24
  let y = 120
  // #ifdef H5
  const menuWidth = 118
  const menuHeight = canQuoteMessage(message) ? 126 : 86
  const menuPoint = resolveContextMenuPoint(point)
  const clientX = menuPoint?.clientX || 0
  const clientY = menuPoint?.clientY || 0
  x = Math.max(12, Math.min(clientX || 24, window.innerWidth - menuWidth - 12))
  y = Math.max(12, Math.min(clientY || 120, window.innerHeight - menuHeight - 12))
  // #endif
  messageContextMenu.value = { message, x, y }
}

function copyContextMessage() {
  const message = messageContextMenu.value?.message
  closeMessageMenu()
  if (!message) return
  const data = clipboardMessageContent(message)
  if (!data) return
  uni.setClipboardData({
    data,
    success() {
      uni.showToast({ title: '已复制', icon: 'none' })
    }
  })
}

function quoteContextMessage() {
  const message = messageContextMenu.value?.message
  closeMessageMenu()
  if (!message || !canQuoteMessage(message)) return
  replyTarget.value = {
    messageId: message.id,
    author: message.author,
    content: copyableMessageContent(message)
  }
}

function hideContextMessage() {
  const message = messageContextMenu.value?.message
  closeMessageMenu()
  if (!message || !canHideMessage(message)) return
  uni.showModal({
    title: 'Delete message',
    content: 'This only hides the message from this user view. The server, support and admin records are kept.',
    confirmText: 'Delete',
    confirmColor: '#d64242',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await store.hideRecord({
          targetType: 'MESSAGE',
          targetId: message.id,
          hiddenScope: 'SINGLE'
        })
        uni.showToast({ title: 'Deleted from view', icon: 'none' })
      } catch (error) {
        uni.showToast({ title: error instanceof Error ? error.message : 'Delete failed', icon: 'none' })
      }
    }
  })
}

async function selectCustomer(conv: SupportConversationItem) {
  showHeaderMenu.value = false
  showComposerTools.value = false
  closeMessageMenu()
  clearReplyTarget()
  showMobileProfile.value = false
  activeConversationId.value = conv.conversationId
  store.setActiveSupportConversation(conv.conversationId)
  store.clearSupportUnread(conv.conversationId)
  store.refreshSupportCustomerProfile(conv.conversationId).catch((error) => {
    console.error('Load customer profile failed:', error)
  })
  showChat.value = true
  enableAudio()
  connectSocket()

  // 标记消息为已读，清除未读角标
  try {
    await store.markSupportRead()
  } catch (error) {
    console.error('标记已读失败:', error)
    uni.showToast({ title: '标记已读失败', icon: 'none' })
  }
}

function selectSearchCustomer(conversationId: string) {
  const conversation = store.state.supportConversations.find(item => item.conversationId === conversationId)
  if (conversation) {
    selectCustomer(conversation)
  }
}

function selectMessageSearchResult(conversationId: string) {
  selectSearchCustomer(conversationId)
}

function selectLedgerCustomer(conversationId: string) {
  const conversation = store.state.supportConversations.find(item => item.conversationId === conversationId)
  if (conversation) {
    selectCustomer(conversation)
  }
}

async function changeOrderStatus(orderId: string, status: TransactionItem['status']) {
  try {
    await store.updateTransactionStatus(orderId, status)
    uni.showToast({ title: workbenchText.value.orderUpdated, icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : workbenchText.value.orderUpdateFailed, icon: 'none' })
  }
}

async function cancelSelectedOrder() {
  const order = selectedOrder.value
  if (!order) return
  const reasons = ['Bad card', 'Wrong code', 'Unclear image', 'Customer canceled', 'Duplicate submission', 'Other']
  uni.showActionSheet({
    itemList: reasons,
    async success(result) {
      const reason = reasons[result.tapIndex] || 'Bad card'
      try {
        await store.cancelTransaction(order.id, {
          reason,
          notifyCustomer: true
        })
        uni.showToast({ title: workbenchText.value.orderCanceled, icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error instanceof Error ? error.message : workbenchText.value.orderCancelFailed, icon: 'none' })
      }
    }
  })
}

async function changeWithdrawalStatus(withdrawalId: string, status: WithdrawalItem['status']) {
  try {
    await store.updateWithdrawalStatus(withdrawalId, status)
    uni.showToast({ title: workbenchText.value.withdrawalUpdated, icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : workbenchText.value.withdrawalUpdateFailed, icon: 'none' })
  }
}

function toggleWorkbenchLanguage() {
  workbenchLanguage.value = workbenchLanguage.value === 'zh' ? 'en' : 'zh'
  uni.setStorageSync(WORKBENCH_LANGUAGE_KEY, workbenchLanguage.value)
}

function statusText(status?: string) {
  const key = (status || '').toLowerCase()
  return statusCopy[key]?.[workbenchLanguage.value] || status || '-'
}

function handleOrderPick(event: { detail: { value: number | string } }) {
  const index = Number(event.detail.value)
  const order = profile.value?.orders[index]
  if (order) {
    selectedOrderId.value = order.id
  }
}

function backToList() {
  showMobileProfile.value = false
  showChat.value = false
}

function showCustomerProfile() {
  showHeaderMenu.value = false
  showComposerTools.value = false
  closeMessageMenu()
  showMobileProfile.value = true
  showChat.value = true
}

function closeMobileProfile() {
  showMobileProfile.value = false
  showChat.value = true
}

function getLastMessage(conv: SupportConversationItem) {
  const last = conv.messages[conv.messages.length - 1]
  if (!last) return '暂无消息'
  if (last.type === 'image') return '[图片]'
  if (last.type === 'gif') return '[GIF]'
  if (last.type === 'voice') return '[语音]'
  if (last.type === 'video') return isVideoFileMessage(last) ? '[视频]' : '[视频通话]'
  return last.content.length > 20 ? `${last.content.slice(0, 20)}...` : last.content
}

function parseVideoCallMessage(message: ChatMessage) {
  if (message.type !== 'video') return null
  try {
    const payload = JSON.parse(message.content) as Partial<VideoCallMessagePayload>
    if (payload.kind !== 'video_call' || !payload.sessionId || !payload.roomId) {
      return null
    }
    return payload as VideoCallMessagePayload
  } catch {
    const match = message.content.match(/Room\s+([A-Za-z0-9_-]+)/i)
    if (!match) return null
    return {
      kind: 'video_call',
      sessionId: '',
      roomId: match[1],
      channelType: 'support',
      channelId: activeConversationId.value,
      initiatorUsername: '',
      receiverUsername: ''
    } as VideoCallMessagePayload
  }
}

function videoCallSession(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return null
  return store.state.videoSessions.find((session) => session.id === payload.sessionId) || null
}

function videoCallStatus(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (payload?.sessionId && localVideoStatuses.value[payload.sessionId]) {
    return localVideoStatuses.value[payload.sessionId]
  }
  return videoCallSession(message)?.status || 'created'
}

function videoCallStatusLabel(message: ChatMessage) {
  return {
    created: '等待接听',
    joining: '正在加入',
    active: '通话中',
    ended: '已结束',
    missed: '未接听',
    rejected: '已拒绝'
  }[videoCallStatus(message)]
}

function videoCallCaption(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const status = videoCallStatus(message)
  if (!payload) return '视频通话邀请格式异常，请刷新后重试。'
  if (status === 'active') return '通话正在进行中，可继续进入同一房间。'
  if (status === 'created') return '对方尚未接听，客服和客户都可以在有效期内进入。'
  if (status === 'joining') return '对方正在加入，请稍候。'
  if (status === 'missed') return '本次呼叫未接通，可以重新发起视频通话。'
  if (status === 'rejected') return '本次呼叫已被拒绝。'
  return '本次视频通话已结束。'
}

function videoCallTitle(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const currentUsername = store.state.currentUser?.username
  if (!payload) return '视频通话邀请'
  if (payload.initiatorUsername === currentUsername) return '你发起了视频通话'
  return `${payload.initiatorUsername || '对方'} 正在邀请你视频通话`
}

function videoCallRoom(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  return `房间 ${payload?.roomId || '-'}`
}

function canAnswerVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const status = videoCallStatus(message)
  return Boolean(payload?.sessionId)
    && payload?.initiatorUsername !== store.state.currentUser?.username
    && (status === 'created' || status === 'joining')
}

function canRejectVideoMessage(message: ChatMessage) {
  return canAnswerVideoMessage(message)
}

function canEnterVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  const status = videoCallStatus(message)
  return Boolean(payload?.sessionId) && ['created', 'joining', 'active'].includes(status)
}

function setLocalVideoStatus(sessionId: string, status: VideoSessionItem['status']) {
  localVideoStatuses.value = {
    ...localVideoStatuses.value,
    [sessionId]: status
  }
}

async function answerVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return
  try {
    setLocalVideoStatus(payload.sessionId, 'joining')
    await store.updateVideoSessionStatus(payload.sessionId, 'joining')
    await openVideoSession(payload.sessionId)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '无法加入通话', icon: 'none' })
  }
}

async function rejectVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return
  try {
    const updated = await store.updateVideoSessionStatus(payload.sessionId, 'rejected')
    setLocalVideoStatus(payload.sessionId, updated.status)
    uni.showToast({ title: '已拒绝视频通话', icon: 'none' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '拒绝失败', icon: 'none' })
  }
}

async function enterVideoMessage(message: ChatMessage) {
  const payload = parseVideoCallMessage(message)
  if (!payload?.sessionId) return
  try {
    await openVideoSession(payload.sessionId)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '无法加入通话', icon: 'none' })
  }
}

async function openVideoSession(sessionId: string) {
  const bootstrap = await store.getVideoSessionBootstrap(sessionId)
  const encoded = encodeURIComponent(JSON.stringify(bootstrap))
  uni.navigateTo({ url: `/pages/video-call/index?bootstrap=${encoded}` })
}

function customerDisplayName(conv: SupportConversationItem) {
  return conv.agentNote?.trim() || conv.customerUsername
}

function customerAvatar(conv: SupportConversationItem) {
  return conv.customerAvatarUrl ? resolveMediaUrl(conv.customerAvatarUrl) : uiIcons.user
}

function displayUnreadCount(conv: SupportConversationItem) {
  return conv.conversationId === activeConversationId.value ? 0 : conv.unreadCount
}

function editCustomerNote() {
  const customer = activeCustomer.value
  if (!customer) {
    uni.showToast({ title: '请先选择客户', icon: 'none' })
    return
  }
  uni.showModal({
    title: '客户备注',
    editable: true,
    placeholderText: '输入客户备注',
    content: customer.agentNote || '',
    confirmText: '保存',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await store.updateSupportNote(customer.conversationId, result.content || '')
        uni.showToast({ title: '备注已保存', icon: 'success' })
      } catch (error) {
        uni.showToast({ title: error instanceof Error ? error.message : '备注保存失败', icon: 'none' })
      }
    }
  })
}

function formatTime(time?: string) {
  // 简化时间显示逻辑
  return time || ''
}

function isMine(message: ChatMessage) {
  return message.author === 'me'
}

function translationFor(message: ChatMessage) {
  if (!isAgent.value || message.author === 'me' || message.author === 'system' || message.type !== 'text') {
    return ''
  }
  return translations[message.id] || ''
}

function shouldTranslate(message: ChatMessage) {
  return isAgent.value
    && message.author !== 'me'
    && message.author !== 'system'
    && message.type === 'text'
    && !!message.content.trim()
    && !translations[message.id]
    && !translatingIds.has(message.id)
}

function translateVisibleIncomingMessages() {
  conversation.value
    .filter(shouldTranslate)
    .forEach((message) => {
      translatingIds.add(message.id)
      translateToChinese(message.content)
        .then((result) => {
          if (result.translatedText && result.translatedText.trim() !== message.content.trim()) {
            translations[message.id] = result.translatedText
          }
        })
        .catch(() => {
          // Translation is a helper; chat should stay usable if the free service is unavailable.
        })
        .finally(() => {
          translatingIds.delete(message.id)
        })
    })
}

function isVideoFileMessage(message: ChatMessage) {
  return message.attachments?.some(attachment => attachment.type === 'video') || false
}

async function changeLotteryFulfillmentStatus(orderId: string, status: LotteryFulfillmentItem['status']) {
  try {
    await store.updateLotteryFulfillmentStatus(orderId, status)
    uni.showToast({ title: 'Prize delivery updated', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : 'Prize delivery update failed', icon: 'none' })
  }
}

function goAdminConsole() {
  uni.redirectTo({ url: '/pages/admin-console/index' })
}

function connectSocket() {
  closeSocket()
  const conversationId = activeConversationId.value
  if (!conversationId) return

  socketStatus.value = 'connecting'
  try {
    socketTask.value = connectChatSocket('support', conversationId, (payload) => {
      if (isReadReceipt(payload)) {
        if (payload.readerUsername !== store.state.currentUser?.username) {
          store.applySupportReadReceipt(conversationId)
        }
        return
      }

      if (isPresenceEvent(payload)) {
        store.applySupportPresence(payload.channelId, payload.online)
        refreshPresence()
        return
      }

      // 处理视频邀请
      if (isVideoInvite(payload)) {
        handleVideoInvite(payload)
        return
      }

      if (isVideoSessionStatus(payload)) {
        handleVideoSessionStatus(payload)
        return
      }

      // 处理普通消息
      if (shouldPlayIncomingSound(payload, conversationId)) {
        playIncomingSound()
      }
      store.pushSupportRealtime(payload, conversationId)
      scrollMessagesToBottom()
      store.markSupportRead().catch(() => {})
    }, {
      onOpen: () => {
        socketStatus.value = 'online'
        store.recoverSupportMessages(conversationId).catch(() => {})
        refreshPresence()
      },
      onClose: () => {
        socketStatus.value = 'offline'
        refreshPresence()
      },
      onError: () => {
        socketStatus.value = 'offline'
        refreshPresence()
      },
      onReconnect: () => {
        socketStatus.value = 'connecting'
        store.recoverSupportMessages(conversationId).catch(() => {})
      }
    })
  } catch (error) {
    console.error('WebSocket连接失败:', error)
    socketStatus.value = 'offline'
  }
}

function startPresenceRefresh() {
  stopPresenceRefresh()
  presenceRefreshTimer.value = setInterval(() => {
    refreshPresence()
  }, 5000)
}

function stopPresenceRefresh() {
  if (presenceRefreshTimer.value) {
    clearInterval(presenceRefreshTimer.value)
    presenceRefreshTimer.value = null
  }
}

function refreshPresence() {
  store.refreshSupport().catch(() => {})
}

function scrollMessagesToBottom() {
  nextTick(() => {
    messageScrollTarget.value = ''
    nextTick(() => {
      messageScrollTarget.value = 'msg-bottom'
    })
  })
}

function stopReadRefresh() {
  if (!readRefreshTimer.value) return
  clearInterval(readRefreshTimer.value)
  readRefreshTimer.value = null
}

function startReadRefresh() {
  stopReadRefresh()
  const startedAt = Date.now()
  readRefreshTimer.value = setInterval(async () => {
    if (Date.now() - startedAt > 12000) {
      stopReadRefresh()
      return
    }
    try {
      await store.refreshSupport()
      const hasPendingOwnMessage = conversation.value.some((message) => message.author === 'me' && message.readState === 'sent')
      if (!hasPendingOwnMessage) {
        stopReadRefresh()
      }
    } catch {
      // keep current chat state if refresh fails
    }
  }, 1500)
}

function closeSocket() {
  socketTask.value?.close({})
  socketTask.value = null
}

function shouldPlayIncomingSound(message: ChatRealtimePayload, conversationId: string) {
  // 视频邀请不播放提示音
  if ('eventType' in message) return false
  return isAgent.value && message.author !== 'me' && message.author !== 'system' && conversationId
}

function isReadReceipt(payload: ChatRealtimePayload): payload is ChatReadReceiptEvent {
  return 'eventType' in payload && payload.eventType === 'read'
}

function isVideoInvite(payload: ChatRealtimePayload): payload is VideoInviteEvent {
  return 'eventType' in payload && payload.eventType === 'video_invite'
}

function isVideoSessionStatus(payload: ChatRealtimePayload): payload is VideoSessionStatusEvent {
  return 'eventType' in payload && payload.eventType === 'video_session_status'
}

function isPresenceEvent(payload: ChatRealtimePayload): payload is PresenceEvent {
  return 'eventType' in payload && payload.eventType === 'presence'
}

function handleVideoSessionStatus(event: VideoSessionStatusEvent) {
  const updated = store.applyVideoSessionStatus(event)
  setLocalVideoStatus(event.sessionId, updated?.status || event.status)
  if (incomingVideoInvite.value?.sessionId === event.sessionId && isTerminalVideoStatus(event.status)) {
    incomingVideoInvite.value = null
  }
}

function handleVideoInvite(invite: VideoInviteEvent) {
  if (invite.channelId !== activeConversationId.value) return
  if (handledVideoInvites.has(invite.sessionId)) return
  handledVideoInvites.add(invite.sessionId)

  const currentUsername = store.state.currentUser?.username
  if (!currentUsername || invite.initiatorUsername === currentUsername) return

  incomingVideoInvite.value = invite
}

function isTerminalVideoStatus(status: VideoSessionItem['status']) {
  return ['ended', 'missed', 'rejected'].includes(status)
}

async function declineIncomingVideo() {
  const invite = incomingVideoInvite.value
  if (!invite) return
  incomingVideoInvite.value = null
  await store.updateVideoSessionStatus(invite.sessionId, 'rejected').catch(() => {})
  setLocalVideoStatus(invite.sessionId, 'rejected')
}

async function answerIncomingVideo() {
  const invite = incomingVideoInvite.value
  if (!invite) return
  incomingVideoInvite.value = null
  try {
    const session = await store.updateVideoSessionStatus(invite.sessionId, 'joining')
    setLocalVideoStatus(invite.sessionId, session.status)
    if (isTerminalVideoStatus(session.status)) {
      uni.showToast({ title: 'Call is no longer available', icon: 'none' })
      return
    }
    await openVideoSession(invite.sessionId)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : 'Unable to join call', icon: 'none' })
  }
}

function enableAudio() {
  audioUnlocked.value = true
}

function toggleSound() {
  audioEnabled.value = !audioEnabled.value
  showHeaderMenu.value = false
  uni.setStorageSync(SOUND_ENABLED_KEY, audioEnabled.value)
  uni.showToast({
    title: audioEnabled.value ? '提示音已开启' : '提示音已关闭',
    icon: 'none'
  })
}

function toggleMoreMenu() {
  showHeaderMenu.value = !showHeaderMenu.value
  showComposerTools.value = false
  closeMessageMenu()
}

function handleHeaderMenu(action: 'note' | 'broadcast' | 'profile') {
  showHeaderMenu.value = false
  if (action === 'profile') {
    showCustomerProfile()
    return
  }
  if (action === 'note') {
    editCustomerNote()
    return
  }
  openBroadcastPanel()
}

function openBroadcastPanel() {
  showHeaderMenu.value = false
  showComposerTools.value = false
  closeMessageMenu()
  showBroadcastPanel.value = true
  if (!broadcastDraft.value.trim() && draft.value.trim()) {
    broadcastDraft.value = draft.value.trim()
  }
}

function closeBroadcastPanel() {
  showBroadcastPanel.value = false
}

function toggleBroadcastCountry(code: string) {
  const index = broadcastCountryCodes.value.indexOf(code)
  if (index >= 0) {
    broadcastCountryCodes.value.splice(index, 1)
    return
  }
  broadcastCountryCodes.value.push(code)
}

function toggleBroadcastCustomer(conversationId: string) {
  const index = broadcastSelectedConversationIds.value.indexOf(conversationId)
  if (index >= 0) {
    broadcastSelectedConversationIds.value.splice(index, 1)
    return
  }
  broadcastSelectedConversationIds.value.push(conversationId)
}

function selectAllBroadcastResults() {
  broadcastSelectedConversationIds.value = broadcastFilteredConversations.value.map(item => item.conversationId)
}

function clearBroadcastSelection() {
  broadcastSelectedConversationIds.value = []
}

function toggleComposerTools() {
  showComposerTools.value = !showComposerTools.value
  showHeaderMenu.value = false
  closeMessageMenu()
  if (showComposerTools.value && isMobile.value) {
    nextTick(() => scrollMessagesToBottom())
  }
}

function closeComposerTools() {
  showComposerTools.value = false
}

function closeChatPanels() {
  closeMessageMenu()
  closeComposerTools()
}

function chooseComposerTool(action: 'image' | 'gif') {
  showComposerTools.value = false
  if (action === 'image') {
    sendImage()
    return
  }
  sendGif()
}

function playIncomingSound() {
  if (!audioEnabled.value || !audioUnlocked.value) return
  // #ifdef H5
  try {
    const context = new (window.AudioContext || (window as any).webkitAudioContext)()
    const oscillator = context.createOscillator()
    const gain = context.createGain()
    oscillator.type = 'sine'
    oscillator.frequency.value = 880
    gain.gain.value = 0.06
    oscillator.connect(gain)
    gain.connect(context.destination)
    oscillator.start()
    oscillator.stop(context.currentTime + 0.16)
  } catch {}
  // #endif
}

function attachPasteListener() {
  // #ifdef H5
  document.addEventListener('paste', handlePasteImage)
  // #endif
}

function detachPasteListener() {
  // #ifdef H5
  document.removeEventListener('paste', handlePasteImage)
  // #endif
}

function applyPendingSupportDraft() {
  const pendingDraft = uni.getStorageSync('pending-support-draft') as string | undefined
  if (pendingDraft && !draft.value.trim()) {
    draft.value = pendingDraft
  }
  uni.removeStorageSync('pending-support-draft')
}

async function sendPendingSupportImage() {
  const pendingImage = uni.getStorageSync('pending-support-image') as string | undefined
  if (!pendingImage) return false
  try {
    await store.sendSupport(pendingImage, 'image')
    uni.removeStorageSync('pending-support-image')
    return true
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : 'Image send failed', icon: 'none' })
    return false
  }
}

async function handlePasteImage(event: ClipboardEvent) {
  const item = Array.from(event.clipboardData?.items || []).find(entry => entry.type.startsWith('image/'))
  const file = item?.getAsFile()
  if (!file) return

  event.preventDefault()
  try {
    addFile(file)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : 'Unsupported image', icon: 'none' })
  }
}

async function handleSend() {
  showHeaderMenu.value = false
  showComposerTools.value = false
  closeMessageMenu()
  if (activeAttachment.value) {
    await sendPendingAttachment()
    return
  }
  const value = draft.value.trim()
  if (!value) return

  try {
    const replyTo = replyTarget.value || undefined
    await store.sendSupport(value, 'text', replyTo)
    draft.value = ''
    clearReplyTarget()
    await sendPendingSupportImage()
    scrollMessagesToBottom()
    startReadRefresh()
  } catch (error) {
    uni.showToast({ title: 'Send failed. Tap Retry.', icon: 'none' })
  }
}

async function retryMessage(message: ChatMessage) {
  try {
    await store.retrySupportMessage(message.id)
    scrollMessagesToBottom()
    startReadRefresh()
  } catch (error) {
    uni.showToast({ title: 'Retry failed', icon: 'none' })
  }
}

async function sendPendingAttachment() {
  const attachment = activeAttachment.value
  if (!attachment || isAttachmentUploading.value) return
  setStatus(attachment.id, 'uploading')
  try {
    const asset = await uploadImage(attachment.url)
    const replyTo = replyTarget.value || undefined
    await store.sendSupport(asset.publicUrl, attachment.kind, replyTo)
    clearReplyTarget()
    scrollMessagesToBottom()
    startReadRefresh()
    clearAttachment(attachment.id)
    if (draft.value.trim()) {
      await handleSend()
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Send failed'
    setStatus(attachment.id, 'failed', message)
    uni.showToast({ title: message, icon: 'none' })
  }
}

async function sendImage() {
  try {
    const filePath = await chooseImageOnce('image')
    if (!filePath) return
    addPath(filePath, 'image')
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '图片选择失败', icon: 'none' })
  }
}

async function sendGif() {
  try {
    const filePath = await chooseImageOnce('gif')
    if (!filePath) return
    addPath(filePath, 'gif')
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : 'GIF选择失败', icon: 'none' })
  }
}

function chooseImageOnce(kind: ComposerAttachmentKind) {
  // #ifdef H5
  return chooseBrowserImageOnce(kind)
  // #endif
  return new Promise<string | null>((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      success(result) {
        resolve(result.tempFilePaths?.[0] || null)
      },
      fail(error) {
        reject(error)
      }
    })
  })
}

function chooseBrowserImageOnce(kind: ComposerAttachmentKind) {
  return new Promise<string | null>((resolve, reject) => {
    if (typeof document === 'undefined') {
      resolve(null)
      return
    }

    const input = document.createElement('input')
    input.type = 'file'
    input.accept = kind === 'gif' ? 'image/gif' : 'image/*'
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    document.body.appendChild(input)

    input.onchange = () => {
      const file = input.files?.[0]
      const filePath = file ? URL.createObjectURL(file) : null
      input.remove()
      resolve(filePath)
    }
    input.onerror = (event) => {
      input.remove()
      reject(event)
    }
    input.click()
  })
}

async function submitFilteredBroadcast() {
  const content = broadcastDraft.value.trim()
  if (!content && !broadcastMediaUrl.value) {
    uni.showToast({ title: '请输入群发内容', icon: 'none' })
    return
  }

  try {
    const selectedTargets = broadcastSelectedConversationIds.value
    const confirmed = await confirmSupportBroadcast(content, selectedTargets)
    if (!confirmed) return
    const broadcast = await store.createBroadcast({
      scope: 'own',
      content,
      messageType: broadcastMediaUrl.value ? broadcastMediaType.value : 'text',
      mediaUrl: broadcastMediaUrl.value || undefined,
      countryCodes: broadcastCountryCodes.value,
      keyword: broadcastKeyword.value.trim(),
      targetConversationIds: selectedTargets.length > 0 ? selectedTargets : undefined
    })
    if (draft.value.trim() === content) {
      draft.value = ''
    }
    broadcastDraft.value = ''
    clearSupportBroadcastMedia()
    broadcastKeyword.value = ''
    broadcastSelectedConversationIds.value = []
    closeBroadcastPanel()
    await store.refreshSupport().catch(() => {})
    uni.showToast({ title: `已群发给 ${broadcast.deliveredCount} 位客户`, icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '群发失败', icon: 'none' })
  }
}

function confirmSupportBroadcast(content: string, selectedTargets: string[]) {
  const countries = broadcastCountryCodes.value.length ? broadcastCountryCodes.value.join(', ') : '全部国家码'
  const keyword = broadcastKeyword.value.trim() || '无关键词'
  const targetCount = selectedTargets.length > 0 ? selectedTargets.length : broadcastFilteredConversations.value.length
  return new Promise<boolean>((resolve) => {
    uni.showModal({
      title: '确认群发',
      content: `国家码：${countries}\n关键词：${keyword}\n客户数：${targetCount}\n媒体：${broadcastMediaUrl.value ? (broadcastMediaType.value === 'image' ? '图片' : '视频') : '无'}\n内容：${content || '（无说明文字）'}`,
      confirmText: '发送',
      cancelText: '取消',
      success(result) {
        resolve(Boolean(result.confirm))
      },
      fail() {
        resolve(false)
      }
    })
  })
}

function chooseSupportBroadcastImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success(result) {
      const filePath = result.tempFilePaths[0]
      if (filePath) uploadSupportBroadcastMedia('image', filePath)
    }
  })
}

function chooseSupportBroadcastVideo() {
  uni.chooseVideo({
    sourceType: ['album', 'camera'],
    compressed: false,
    success(result) {
      if (result.tempFilePath) uploadSupportBroadcastMedia('video', result.tempFilePath)
    }
  })
}

async function uploadSupportBroadcastMedia(type: 'image' | 'video', filePath: string) {
  broadcastUploading.value = true
  try {
    const asset = type === 'image' ? await uploadImage(filePath) : await uploadVideo(filePath)
    broadcastMediaType.value = type
    broadcastMediaUrl.value = asset.publicUrl
    broadcastMediaName.value = asset.originalName || (type === 'image' ? '群发图片' : '群发视频')
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '附件上传失败', icon: 'none' })
  } finally {
    broadcastUploading.value = false
  }
}

function clearSupportBroadcastMedia() {
  broadcastMediaUrl.value = ''
  broadcastMediaName.value = ''
  broadcastMediaType.value = 'image'
}

async function startVideoCall() {
  if (!activeConversationId.value) {
    uni.showToast({ title: '请先选择客户', icon: 'none' })
    return
  }
  try {
    const bootstrap = await store.createVideoSession({
      channelType: 'support',
      channelId: activeConversationId.value
    })
    setLocalVideoStatus(bootstrap.session.id, bootstrap.session.status)
    await store.refreshSupport().catch(() => {})
    const encoded = encodeURIComponent(JSON.stringify(bootstrap))
    uni.navigateTo({ url: `/pages/video-call/index?bootstrap=${encoded}` })
  } catch (error) {
    uni.showToast({
      title: error instanceof Error ? error.message : '视频通话启动失败',
      icon: 'none'
    })
  }
}


function previewImage(url: string) {
  const resolved = resolveMediaUrl(url)
  uni.previewImage({ urls: [resolved], current: resolved })
}
</script>

<style scoped lang="scss">
.chat-container {
  --chat-doodle-pattern: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='220' height='220' viewBox='0 0 220 220'%3E%3Cg fill='none' stroke='%23606C38' stroke-opacity='.18' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M18 28c10-12 27-6 27 8 0 13-17 21-27 7-4-6-4-10 0-15Z'/%3E%3Cpath d='M78 18h22v18H78zM85 18v-6M93 18v-6M82 44h14'/%3E%3Cpath d='M142 22c14 2 24 13 21 26-2 11-13 18-27 14-11-4-16-14-12-25 3-9 10-16 18-15Z'/%3E%3Cpath d='M35 92c18-5 35 4 42 21M24 110c18 12 38 15 60 8'/%3E%3Cpath d='M117 90l24 16-24 16zM151 94l12 8M151 118l12 8'/%3E%3Cpath d='M185 82c8 0 15 7 15 15s-7 15-15 15-15-7-15-15 7-15 15-15Z'/%3E%3Cpath d='M31 169c11-13 31-12 42 2M34 188c13 8 26 8 39 0'/%3E%3Cpath d='M106 162c11-9 29-7 36 5 7 13 0 27-14 29-14 2-25-6-25-18 0-7 1-12 3-16Z'/%3E%3Cpath d='M172 164h24v24h-24zM178 170h12M178 176h12M178 182h8'/%3E%3Cpath d='M62 57l11 11M73 57 62 68M198 30l8 8M206 30l-8 8M92 136l9 9M101 136l-9 9'/%3E%3C/g%3E%3C/svg%3E");
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
  background: linear-gradient(145deg, #d8edbf 0%, #b9ddab 42%, #85bea9 100%);
}

/* ============ 左侧客户列表 ============ */
.customer-sidebar {
  width: 304px;
  flex: 0 0 304px;
  min-width: 240px;
  background: #f7f9fb;
  border-right: 1px solid rgba(136, 153, 166, 0.22);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s;
  backdrop-filter: blur(10px);
}

.customer-profile-panel {
  width: 360px;
  flex: 0 1 360px;
  min-width: 280px;
  background: #ffffff;
  border-left: 1px solid rgba(136, 153, 166, 0.22);
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(10px);
}

.profile-hidden {
  display: none;
}

.profile-header {
  min-height: 78px;
  padding: 15px 16px;
  border-bottom: 1px solid rgba(90, 123, 89, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.profile-back-btn {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(33, 56, 43, 0.06);
  color: #26352b;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
}

.profile-header-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.language-toggle {
  min-width: 58px;
  height: 26px;
  padding: 0 9px;
  border-radius: 8px;
  border: 1px solid rgba(0, 136, 204, 0.24);
  background: rgba(0, 136, 204, 0.08);
  color: #0088cc;
  font-size: 12px;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
}

.language-toggle:active {
  background: rgba(0, 136, 204, 0.16);
}

.profile-eyebrow {
  display: block;
  font-size: 11px;
  color: #6c7d6f;
  text-transform: uppercase;
  font-weight: 800;
}

.profile-title {
  display: block;
  margin-top: 4px;
  font-size: 18px;
  color: #1f2d24;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.profile-status {
  padding: 4px 8px;
  border-radius: 8px;
  background: rgba(18, 201, 107, 0.12);
  color: #0a7a44;
  font-size: 12px;
  font-weight: 800;
}

.profile-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px 18px;
}

.profile-section {
  padding: 14px 0;
  border-bottom: 1px solid rgba(90, 123, 89, 0.1);
}

.profile-section:first-child {
  padding-top: 0;
}

.profile-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  min-width: 0;
}

.profile-label,
.metric-label {
  font-size: 12px;
  color: #718075;
  flex-shrink: 0;
}

.profile-value {
  min-width: 0;
  max-width: 100%;
  font-size: 13px;
  color: #243027;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.metric-item {
  min-width: 0;
  padding: 11px 9px;
  border-radius: 8px;
  background: #eef8ec;
}

.metric-value {
  display: block;
  margin-top: 5px;
  font-size: 15px;
  color: #173321;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.ledger-section {
  padding-top: 0;
  max-height: 300px;
  overflow: hidden;
}

.ledger-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.ledger-summary-item {
  min-width: 0;
  padding: 9px 7px;
  border-radius: 8px;
  background: rgba(231, 246, 229, 0.82);
}

.ledger-row {
  margin-top: 8px;
  padding: 9px 10px;
  border: 1px solid rgba(90, 123, 89, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
}

.ledger-row.active {
  border-color: rgba(18, 201, 107, 0.35);
  background: rgba(219, 249, 215, 0.78);
}

.ledger-row-main,
.ledger-row-money {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ledger-row-money {
  align-items: flex-end;
  flex-shrink: 0;
}

.ledger-name {
  max-width: 140px;
  color: #203025;
  font-size: 13px;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ledger-meta,
.ledger-pending {
  color: #718075;
  font-size: 11px;
  font-weight: 700;
}

.ledger-money {
  color: #14854d;
  font-size: 13px;
  font-weight: 900;
}

.section-head,
.work-top,
.work-actions {
  display: flex;
  align-items: center;
}

.section-head,
.work-top {
  justify-content: space-between;
  gap: 10px;
}

.section-title {
  font-size: 15px;
  font-weight: 900;
  color: #243027;
}

.section-count {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  background: #eef5eb;
  color: #4d6d54;
  font-size: 12px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.order-focus {
  margin-top: 10px;
}

.order-picker {
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid rgba(90, 123, 89, 0.18);
  border-radius: 8px;
  background: rgba(238, 247, 235, 0.84);
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-picker-label {
  flex-shrink: 0;
  color: #718075;
  font-size: 12px;
  font-weight: 700;
}

.order-picker-value {
  min-width: 0;
  flex: 1;
  color: #203025;
  font-size: 13px;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-picker-arrow {
  flex-shrink: 0;
  color: #607664;
  font-size: 16px;
  font-weight: 900;
}

.work-item {
  margin-top: 10px;
  padding: 11px;
  border: 1px solid rgba(90, 123, 89, 0.15);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.selected-work-item {
  background: rgba(255, 255, 255, 0.9);
}

.work-item.compact {
  padding: 8px 10px;
}

.work-title {
  min-width: 0;
  font-size: 13px;
  color: #1f2d24;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-status {
  flex-shrink: 0;
  padding: 3px 7px;
  border-radius: 8px;
  background: #eef1f2;
  color: #536269;
  font-size: 11px;
  font-weight: 800;
}

.work-status.pending,
.work-status.joining {
  background: #fff4cf;
  color: #8b6400;
}

.work-status.processing,
.work-status.active,
.work-status.approved {
  background: #dceeff;
  color: #17609a;
}

.work-status.completed,
.work-status.ended {
  background: #daf7e5;
  color: #0b7b43;
}

.claim-kind {
  display: inline-flex;
  margin-top: 8px;
  padding: 3px 7px;
  border: 1px solid rgba(0, 47, 167, 0.18);
  border-radius: 4px;
  background: #f2f6ff;
  color: #002fa7;
  font-size: 11px;
  font-weight: 800;
}

.physical-claim {
  border-left: 3px solid #e9a900;
}

.address-line {
  overflow-wrap: anywhere;
}

.work-status.disputed,
.work-status.canceled,
.work-status.rejected,
.work-status.missed {
  background: #ffe2e2;
  color: #a53030;
}

.work-status.high {
  background: #ffe2e2;
  color: #a53030;
}

.work-status.medium {
  background: #fff4cf;
  color: #8b6400;
}

.work-status.low {
  background: #daf7e5;
  color: #0b7b43;
}

.work-line {
  display: block;
  margin-top: 5px;
  font-size: 12px;
  color: #68766c;
  overflow-wrap: anywhere;
  line-height: 1.35;
}

.work-line.strong {
  color: #14854d;
  font-weight: 900;
}

.work-actions {
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 9px;
}

.mini-btn {
  min-width: 62px;
  height: 28px;
  padding: 0 9px;
  border: 0;
  border-radius: 8px;
  background: #e8eee7;
  color: #304035;
  font-size: 12px;
  font-weight: 800;
  line-height: 28px;
}

.mini-btn.primary {
  background: #0088cc;
  color: #ffffff;
}

.mini-btn.danger {
  background: #ff6961;
  color: #ffffff;
}

.work-card-line {
  margin-top: 7px;
  display: flex;
  align-items: center;
  gap: 7px;
}

.work-card-line .work-line {
  margin-top: 0;
}

.work-card-logo {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  border: 1px solid #dce3dd;
  background: #f7f7f8;
}

.risk-item {
  border-color: rgba(244, 91, 91, 0.22);
}

.broadcast-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  background: rgba(15, 24, 32, 0.36);
  box-sizing: border-box;
}

.broadcast-dialog {
  width: min(560px, 100%);
  max-height: min(720px, calc(100vh - 36px));
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(17, 31, 45, 0.22);
  box-sizing: border-box;
}

.broadcast-head,
.broadcast-tools,
.broadcast-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.broadcast-head,
.broadcast-tools {
  justify-content: space-between;
}

.broadcast-title {
  font-size: 18px;
  font-weight: 900;
  color: #1f2d24;
}

.broadcast-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #536269;
  text-align: center;
  line-height: 32px;
  font-size: 20px;
  font-weight: 900;
}

.broadcast-textarea,
.broadcast-input {
  width: 100%;
  border: 1px solid rgba(90, 123, 89, 0.2);
  border-radius: 8px;
  background: #f8faf8;
  color: #1f2d24;
  box-sizing: border-box;
}

.broadcast-media-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.broadcast-uploading,
.broadcast-preview-name {
  color: #68766c;
  font-size: 12px;
  font-weight: 800;
}

.broadcast-media-preview {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(90, 123, 89, 0.18);
  border-radius: 8px;
  background: #f7f9fb;
}

.broadcast-preview-image,
.broadcast-preview-video {
  width: min(100%, 420px);
  height: 230px;
  background: #101820;
}

.mini-btn.danger {
  border-color: #e6aaa5;
  background: #fff1f0;
  color: #b42318;
}

.broadcast-textarea {
  min-height: 112px;
  padding: 11px 12px;
  font-size: 14px;
  line-height: 1.5;
}

.broadcast-input {
  height: 40px;
  padding: 0 12px;
  font-size: 13px;
}

.country-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-chip {
  min-width: 72px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(90, 123, 89, 0.22);
  border-radius: 8px;
  background: #ffffff;
  color: #314138;
  font-size: 13px;
  font-weight: 800;
  line-height: 30px;
}

.filter-chip.active {
  border-color: #0088cc;
  background: #e7f5fc;
  color: #006fa8;
}

.broadcast-count {
  color: #68766c;
  font-size: 12px;
  font-weight: 800;
}

.broadcast-target-list {
  min-height: 160px;
  max-height: 260px;
  border: 1px solid rgba(90, 123, 89, 0.15);
  border-radius: 8px;
  background: #fbfcfb;
  overflow: hidden;
}

.broadcast-target {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(90, 123, 89, 0.1);
}

.broadcast-target.active {
  background: #e7f5fc;
}

.broadcast-target-name,
.broadcast-target-meta {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.broadcast-target-name {
  color: #1f2d24;
  font-size: 14px;
  font-weight: 900;
}

.broadcast-target-meta {
  margin-top: 4px;
  color: #68766c;
  font-size: 12px;
}

.broadcast-actions {
  justify-content: flex-end;
}

.mini-empty,
.profile-empty {
  color: #7b887f;
  font-size: 13px;
}

.mini-empty {
  margin-top: 10px;
}

.profile-empty {
  padding: 18px;
  line-height: 1.5;
}

.sidebar-hidden {
  transform: translateX(-100%);
  position: absolute;
  z-index: -1;
}

.sidebar-header {
  padding: 14px 12px;
  border-bottom: 1px solid rgba(136, 153, 166, 0.18);
  background: #f7f7f8;
}

.admin-sidebar-tools {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.admin-console-button {
  width: 100%;
  margin: 0;
  padding: 9px 12px;
  border: 1px solid #002fa7;
  border-radius: 7px;
  background: #002fa7;
  color: #ffffff;
  box-shadow: none;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.2;
}

.agent-filter-block {
  padding: 10px;
  border: 1px solid #d9dde3;
  border-radius: 8px;
  background: #ffffff;
}

.filter-label,
.filter-count {
  display: block;
  font-size: 12px;
  color: #68727d;
  line-height: 1.3;
}

.agent-filter-picker {
  margin-top: 7px;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid #cfd5df;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #101820;
  font-size: 13px;
  font-weight: 800;
  background: #ffffff;
}

.picker-caret {
  color: #002fa7;
  font-size: 14px;
}

.filter-count {
  margin-top: 7px;
}

.search-mode-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.search-mode-pill {
  height: 32px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.72);
  color: #647066;
  font-size: 12px;
  font-weight: 800;
  line-height: 32px;
  text-align: center;
  cursor: pointer;
  border: 1px solid rgba(90, 123, 89, 0.16);
}

.search-mode-pill.active {
  background: #00a884;
  color: #ffffff;
  border-color: #00a884;
}

.search-box {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  padding: 8px 12px;
  border: 1px solid rgba(90, 123, 89, 0.2);
}

.search-icon {
  margin-right: 8px;
  font-size: 16px;
}

.search-input {
  flex: 1;
  font-size: 14px;
  background: transparent;
  border: none;
}

.customer-list-scroll {
  flex: 1;
  overflow-y: auto;
}

.country-group {
  border-bottom: 1px solid rgba(90, 123, 89, 0.08);
}

.country-group-head {
  min-height: 42px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: rgba(244, 249, 244, 0.92);
  cursor: pointer;
  box-sizing: border-box;
}

.country-title,
.country-subtitle {
  display: block;
}

.country-title {
  font-size: 13px;
  font-weight: 900;
  color: #304239;
}

.country-subtitle {
  margin-top: 2px;
  font-size: 11px;
  color: #7a887d;
}

.country-head-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.country-count {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  background: rgba(0, 168, 132, 0.12);
  color: #007c61;
  font-size: 12px;
  font-weight: 900;
  line-height: 22px;
  text-align: center;
  box-sizing: border-box;
}

.country-caret {
  font-size: 16px;
  color: #6d796f;
  transform: rotate(0deg);
  transition: transform 0.16s ease;
}

.country-caret.collapsed {
  transform: rotate(-90deg);
}

.search-result-list {
  display: flex;
  flex-direction: column;
}

.mini-search-state {
  padding: 12px 14px;
  color: #6f7f73;
  font-size: 12px;
  font-weight: 700;
}

.message-search-item {
  padding: 12px 14px;
  border-bottom: 1px solid rgba(90, 123, 89, 0.1);
  cursor: pointer;
}

.message-search-item:hover {
  background: rgba(225, 244, 222, 0.72);
}

.search-snippet {
  display: block;
  margin-top: 6px;
  white-space: normal;
  line-height: 1.35;
}

.avatar-letter {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #dfeee4;
  color: #1f5f49;
  font-size: 18px;
  font-weight: 900;
  line-height: 48px;
  text-align: center;
}

.customer-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid rgba(90, 123, 89, 0.1);
}

.customer-item:hover {
  background: rgba(225, 244, 222, 0.72);
}

.customer-item.active {
  background: #e4f7df;
  border-left: 3px solid #00a884;
}

.customer-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  margin-right: 12px;
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #e0e0e0;
}

.online-dot {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 12px;
  height: 12px;
  background: #07c160;
  border: 2px solid #ffffff;
  border-radius: 50%;
}

.online-dot.offline {
  background: #b8c0ba;
}

.customer-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.customer-name {
  font-size: 16px;
  font-weight: 600;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-time {
  font-size: 12px;
  color: #999999;
  flex-shrink: 0;
  margin-left: 8px;
}

.last-message {
  font-size: 13px;
  color: #999999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.customer-note {
  display: block;
  margin-top: -2px;
  font-size: 12px;
  color: #6f8069;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  background: #ff4d4f;
  color: #ffffff;
  font-size: 12px;
  font-weight: bold;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
  flex-shrink: 0;
}

.empty-list {
  padding: 60px 20px;
  text-align: center;
}

.empty-text {
  font-size: 14px;
  color: #999999;
}

.sidebar-footer {
  display: flex;
  padding: 12px 16px;
  border-top: 1px solid rgba(90, 123, 89, 0.2);
  background: #eef8ec;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: #999999;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: bold;
  color: #333333;
}

/* ============ 右侧聊天区域 ============ */
.chat-main {
  flex: 1;
  min-width: 360px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(145deg, #d8edbf 0%, #b9ddab 42%, #85bea9 100%);
  transition: transform 0.3s;
  backdrop-filter: blur(10px);
}

.chat-hidden {
  transform: translateX(100%);
  position: absolute;
  right: 0;
  width: 100%;
  z-index: -1;
}

.chat-header {
  position: relative;
  z-index: 70;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 66px;
  padding: 10px 18px;
  background: #fbfcfa;
  border-bottom: 1px solid rgba(90, 123, 89, 0.16);
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
}

.back-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 8px;
  cursor: pointer;
}

.header-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e0e0;
  margin-right: 12px;
}

.header-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.header-name {
  font-size: 16px;
  font-weight: 600;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-status {
  font-size: 12px;
  color: #07c160;
  margin-top: 2px;
}

.header-status.offline {
  color: #8a948d;
}

.header-note {
  font-size: 12px;
  color: #6f8069;
  margin-top: 2px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.icon-action {
  position: relative;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(33, 56, 43, 0.04);
  color: #44544b;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease, transform 0.16s ease;
}

.icon-action:hover {
  background: rgba(17, 42, 31, 0.08);
  color: #14291d;
}

.icon-action:active {
  transform: scale(0.96);
}

.icon-action.primary {
  background: rgba(0, 168, 132, 0.13);
  color: #007d65;
}

.icon-action.primary:hover {
  background: rgba(0, 168, 132, 0.18);
  color: #00644f;
}

.icon-action.is-on::after {
  content: '';
  position: absolute;
  right: 7px;
  top: 8px;
  width: 7px;
  height: 7px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  background: #12c96b;
}

.icon-bell {
  position: relative;
  width: 15px;
  height: 15px;
  border: 2px solid currentColor;
  border-top-left-radius: 10px;
  border-top-right-radius: 10px;
  border-bottom: 0;
}

.icon-bell::before {
  content: '';
  position: absolute;
  left: -4px;
  right: -4px;
  bottom: -4px;
  height: 2px;
  border-radius: 2px;
  background: currentColor;
}

.icon-bell::after {
  content: '';
  position: absolute;
  left: 5px;
  bottom: -8px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.icon-video {
  position: relative;
  width: 21px;
  height: 15px;
  border: 2px solid currentColor;
  border-radius: 5px;
  background: transparent;
  box-sizing: border-box;
}

.icon-video::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 4px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.icon-video::after {
  content: '';
  position: absolute;
  right: -8px;
  top: 3px;
  width: 8px;
  height: 7px;
  border-radius: 1px 4px 4px 1px;
  background: currentColor;
  clip-path: polygon(0 18%, 100% 0, 100% 100%, 0 82%);
}

.icon-more {
  position: relative;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: -8px 0 0 currentColor, 8px 0 0 currentColor;
}

.header-menu-wrap {
  position: relative;
  z-index: 80;
}

.header-menu {
  position: absolute;
  z-index: 90;
  right: 0;
  top: 44px;
  width: 164px;
  padding: 6px;
  border: 1px solid rgba(83, 107, 91, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 46px rgba(23, 45, 33, 0.16);
  box-sizing: border-box;
}

.header-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 0 10px;
  border-radius: 6px;
  color: #26352b;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.header-menu-item:hover {
  background: rgba(0, 168, 132, 0.08);
}

.menu-icon {
  position: relative;
  width: 18px;
  height: 18px;
  color: #5d6a62;
  flex-shrink: 0;
}

.menu-icon.note {
  border: 2px solid currentColor;
  border-radius: 4px;
  box-sizing: border-box;
}

.menu-icon.note::after {
  content: '';
  position: absolute;
  left: 4px;
  right: 4px;
  top: 5px;
  height: 2px;
  border-radius: 2px;
  background: currentColor;
  box-shadow: 0 5px 0 currentColor;
}

.menu-icon.profile {
  border: 2px solid currentColor;
  border-radius: 50%;
  box-sizing: border-box;
}

.menu-icon.profile::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 3px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
}

.menu-icon.profile::after {
  content: '';
  position: absolute;
  left: 4px;
  right: 4px;
  bottom: 3px;
  height: 6px;
  border-radius: 8px 8px 2px 2px;
  background: currentColor;
}

.menu-icon.broadcast::before {
  content: '';
  position: absolute;
  left: 1px;
  top: 6px;
  width: 8px;
  height: 6px;
  border-radius: 2px;
  background: currentColor;
}

.menu-icon.broadcast::after {
  content: '';
  position: absolute;
  right: 1px;
  top: 3px;
  width: 6px;
  height: 12px;
  border: 2px solid currentColor;
  border-left: 0;
  border-radius: 0 10px 10px 0;
  box-sizing: border-box;
}

.message-area {
  flex: 1;
  min-height: 0;
  background-image:
    var(--chat-doodle-pattern),
    radial-gradient(circle at 0% 8%, rgba(224, 239, 153, 0.62) 0, rgba(224, 239, 153, 0) 34%),
    linear-gradient(145deg, rgba(216, 237, 191, 0.96) 0%, rgba(185, 221, 171, 0.96) 42%, rgba(133, 190, 169, 0.96) 100%);
  background-size: 220px 220px, cover, cover;
  background-position: center;
  padding: 18px 24px;
  box-sizing: border-box;
  overflow-y: auto;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.date-divider {
  text-align: center;
  margin: 12px 0;
}

.date-divider text {
  display: inline-block;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.78);
  color: #51606d;
  font-size: 12px;
  border-radius: 12px;
}

.message-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-width: 0;
}

.message-wrapper.mine {
  justify-content: flex-end;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e0e0e0;
  flex-shrink: 0;
}

.message-img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  display: block;
}

.video-call-card {
  min-width: 210px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.video-call-title {
  font-size: 15px;
  line-height: 1.35;
  color: #1f2d24;
  font-weight: 900;
}

.video-call-room {
  font-size: 12px;
  line-height: 1.35;
  color: #627169;
  word-break: break-all;
}

.video-call-status {
  align-self: flex-start;
  padding: 3px 8px;
  border-radius: 8px;
  background: rgba(18, 201, 107, 0.12);
  color: #0a7a44;
  font-size: 12px;
  font-weight: 800;
}

.video-call-status.rejected,
.video-call-status.missed,
.video-call-status.ended {
  background: rgba(255, 105, 97, 0.14);
  color: #a53030;
}

.video-call-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 3px;
}

.video-call-btn {
  min-width: 62px;
  height: 28px;
  padding: 0 10px;
  border: 0;
  border-radius: 8px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
  line-height: 28px;
}

.video-call-btn.answer,
.video-call-btn.enter {
  background: #12c96b;
}

.video-call-btn.decline {
  background: #ff6961;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  justify-content: flex-end;
}

.msg-time {
  font-size: 11px;
  color: #999999;
}

.msg-status {
  font-size: 12px;
  color: #07c160;
}

.msg-status.failed {
  color: #d92d20;
  cursor: pointer;
  font-weight: 600;
}

.incoming-call-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(17, 28, 23, 0.48);
  box-sizing: border-box;
}

.incoming-call-dialog {
  width: min(340px, 100%);
  padding: 20px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 46px rgba(20, 36, 29, 0.24);
  box-sizing: border-box;
}

.incoming-call-title,
.incoming-call-copy {
  display: block;
}

.incoming-call-title {
  font-size: 18px;
  font-weight: 800;
  color: #1f3328;
}

.incoming-call-copy {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.45;
  color: #53645b;
  word-break: break-word;
}

.incoming-call-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.incoming-call-btn {
  flex: 1;
  min-width: 0;
  height: 40px;
  border: 0;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
  line-height: 40px;
}

.incoming-call-btn.answer {
  background: #00a884;
}

.incoming-call-btn.decline {
  background: #ff5e57;
}

.image-preview-bar {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: #ffffff;
  border-top: 1px solid #e5e5e5;
  gap: 12px;
}

.preview-thumb {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  object-fit: cover;
}

.preview-text {
  flex: 1;
  font-size: 14px;
  color: #666666;
}

.preview-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  border-radius: 50%;
  background: #f0f0f0;
}

.input-area {
  background: #ffffff;
  border-top: 1px solid rgba(136, 153, 166, 0.22);
  padding: 10px 16px 12px;
  backdrop-filter: blur(10px);
  flex-shrink: 0;
  transition: padding 0.18s ease, box-shadow 0.18s ease;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.composer-tools-wrap {
  position: relative;
  flex-shrink: 0;
}

.composer-tool-main {
  position: relative;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(23, 33, 43, 0.06);
  color: #51606d;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease, transform 0.16s ease;
}

.composer-tool-main:hover,
.composer-tool-main.is-open {
  background: rgba(0, 136, 204, 0.1);
  color: #0088cc;
}

.composer-tool-main:active {
  transform: scale(0.96);
}

.composer-tool-main text:first-child,
.composer-tool-main text:last-child {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 16px;
  height: 2px;
  border-radius: 2px;
  background: currentColor;
  transform: translate(-50%, -50%);
}

.composer-tool-main text:last-child {
  transform: translate(-50%, -50%) rotate(90deg);
}

.composer-popover {
  position: absolute;
  left: 0;
  bottom: 50px;
  width: 176px;
  padding: 8px;
  border: 1px solid rgba(136, 153, 166, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 44px rgba(25, 42, 62, 0.14);
  box-sizing: border-box;
  z-index: 18;
}

.composer-option {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 0 10px;
  border-radius: 7px;
  color: #17212b;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.composer-option:hover {
  background: rgba(0, 136, 204, 0.08);
}

.composer-option-icon {
  position: relative;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: rgba(0, 136, 204, 0.1);
  color: #0088cc;
  flex-shrink: 0;
}

.composer-option-icon.image::before {
  content: '';
  position: absolute;
  left: 6px;
  top: 7px;
  width: 14px;
  height: 11px;
  border: 2px solid currentColor;
  border-radius: 3px;
  box-sizing: border-box;
}

.composer-option-icon.image::after {
  content: '';
  position: absolute;
  left: 9px;
  bottom: 7px;
  width: 10px;
  height: 7px;
  background: linear-gradient(135deg, transparent 0 42%, currentColor 43% 57%, transparent 58%);
}

.composer-option-icon.gif {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0;
}

.composer-panel {
  display: none;
}

.composer-panel-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.composer-panel-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #263642;
  font-size: 12px;
  font-weight: 800;
}

.composer-panel-icon {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: #f0f6f8;
  color: #0088cc;
  box-shadow: inset 0 0 0 1px rgba(0, 136, 204, 0.08);
}

.composer-panel-icon.image::before {
  content: '';
  position: absolute;
  left: 14px;
  top: 16px;
  width: 24px;
  height: 18px;
  border: 2px solid currentColor;
  border-radius: 5px;
  box-sizing: border-box;
}

.composer-panel-icon.image::after {
  content: '';
  position: absolute;
  left: 19px;
  bottom: 16px;
  width: 18px;
  height: 11px;
  background: linear-gradient(135deg, transparent 0 42%, currentColor 43% 57%, transparent 58%);
}

.composer-panel-icon.gif {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0;
}

.reply-composer {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  margin-bottom: 8px;
  padding: 8px 10px;
  border-left: 3px solid #00a884;
  border-radius: 8px;
  background: rgba(232, 246, 239, 0.94);
  box-sizing: border-box;
}

.reply-composer-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.reply-composer-label {
  color: #007a61;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.3;
}

.reply-composer-text {
  margin-top: 2px;
  color: #344b40;
  font-size: 13px;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-composer-close {
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.06);
  color: #42544a;
  font-size: 20px;
  line-height: 26px;
  text-align: center;
  cursor: pointer;
}

.message-menu-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: transparent;
}

.message-context-menu {
  position: fixed;
  min-width: 112px;
  padding: 6px;
  border: 1px solid rgba(38, 59, 48, 0.08);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(23, 40, 31, 0.18);
  box-sizing: border-box;
}

.message-context-item {
  height: 36px;
  padding: 0 14px;
  border-radius: 6px;
  color: #22332a;
  font-size: 14px;
  font-weight: 700;
  line-height: 36px;
  cursor: pointer;
  box-sizing: border-box;
}

.message-context-item:hover {
  background: rgba(0, 168, 132, 0.1);
}

.message-context-item.danger {
  color: #c93636;
}

.message-context-item.danger:hover {
  background: rgba(214, 66, 66, 0.1);
}

.message-input {
  flex: 1;
  min-width: 0;
  height: 42px;
  padding: 0 14px;
  border: 1px solid rgba(80, 116, 93, 0.22);
  border-radius: 8px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.9);
  box-sizing: border-box;
}

.send-btn {
  min-width: 76px;
  height: 42px;
  padding: 0 18px;
  background: rgba(0, 168, 132, 0.2);
  color: #6f8069;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 800;
  line-height: 42px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  box-sizing: border-box;
}

.send-btn.active {
  background: #00a884;
  color: #ffffff;
}

.send-btn.active:hover {
  background: #008c6d;
}

/* ============ 响应式设计 ============ */
@media (max-width: 768px) {
  .chat-container {
    background: linear-gradient(145deg, #d8edbf 0%, #b9ddab 42%, #85bea9 100%);
    overflow: hidden;
  }

  .customer-sidebar {
    width: 100%;
    flex-basis: 100%;
    min-width: 0;
  }

  .sidebar-hidden {
    display: none;
  }

  .chat-hidden {
    display: none;
  }

  .customer-profile-panel {
    width: 100%;
    min-width: 0;
    flex: 1 1 100%;
    border-left: 0;
    height: 100%;
    max-height: 100vh;
  }

  .customer-profile-panel.profile-hidden {
    display: none;
  }

  .profile-header {
    min-height: 58px;
    padding: 8px 10px;
    justify-content: flex-start;
    flex-shrink: 0;
  }

  .profile-header > view:nth-child(2) {
    min-width: 0;
    flex: 1;
  }

  .profile-title {
    font-size: 16px;
  }

  .chat-main {
    min-width: 0;
    width: 100%;
  }

  .chat-header {
    min-height: 58px;
    padding: 8px 10px;
    gap: 8px;
  }

  .header-left,
  .header-info {
    flex: 1;
    min-width: 0;
  }

  .header-avatar {
    width: 34px;
    height: 34px;
    margin-right: 8px;
  }

  .header-name {
    max-width: 42vw;
    font-size: 15px;
  }

  .header-actions {
    gap: 4px;
  }

  .icon-action {
    width: 34px;
    height: 34px;
  }

  .header-menu {
    right: 0;
    width: min(168px, calc(100vw - 24px));
  }

  .message-area {
    padding: 12px 10px;
  }

  .input-area {
    padding: 8px 10px calc(10px + env(safe-area-inset-bottom));
  }

  .input-area.tools-open {
    box-shadow: 0 -14px 32px rgba(25, 42, 62, 0.1);
  }

  .input-row {
    gap: 7px;
  }

  .composer-tool-main {
    width: 36px;
    height: 36px;
  }

  .composer-panel {
    display: block;
    min-height: 132px;
    margin: 10px -10px calc(-10px - env(safe-area-inset-bottom));
    padding: 14px 18px calc(18px + env(safe-area-inset-bottom));
    background: #f7f9fb;
    border-top: 1px solid rgba(136, 153, 166, 0.18);
    box-sizing: border-box;
  }

  .composer-panel-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .composer-panel-icon {
    width: 50px;
    height: 50px;
  }

  .message-input {
    height: 40px;
    padding: 0 11px;
  }

  .send-btn {
    min-width: 58px;
    height: 40px;
    padding: 0 12px;
    line-height: 40px;
    font-size: 13px;
  }
}

@media (min-width: 769px) and (max-width: 1180px) {
  .customer-sidebar {
    width: 260px;
    flex-basis: 260px;
    min-width: 220px;
  }

  .customer-profile-panel {
    width: 310px;
    flex-basis: 310px;
    min-width: 260px;
  }

  .chat-main {
    min-width: 320px;
  }

  .message-area {
    padding: 14px 18px;
  }

  .ledger-section {
    max-height: 260px;
  }
}

@media (min-width: 1181px) {
  .message-list {
    max-width: 920px;
    width: 100%;
    margin: 0 auto;
  }
}
</style>
