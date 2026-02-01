package com.sonique.kotlinytmusicscraper.models.response

import com.sonique.kotlinytmusicscraper.models.AccountInfo
import com.sonique.kotlinytmusicscraper.models.Run
import com.sonique.kotlinytmusicscraper.models.Thumbnail
import kotlinx.serialization.Serializable

@Serializable
data class AccountSwitcherEndpointResponse(
    val code: String?,
    val data: AccountSwitcherData?,
)

@Serializable
data class AccountSwitcherData(
    val actions: List<AccountSwitcherAction?>?,
    val contents: List<AccountSwitcherSection>?,
    val responseContext: AccountSwitcherResponseContext?,
    val selectText: AccountSwitcherSelectText?,
)

@Serializable
data class AccountSwitcherAction(
    val getMultiPageMenuAction: AccountSwitcherGetMultiPageMenuAction?,
)

@Serializable
data class AccountSwitcherGetMultiPageMenuAction(
    val menu: AccountSwitcherMenu?,
)

@Serializable
data class AccountSwitcherMenu(
    val multiPageMenuRenderer: AccountSwitcherMultiPageMenuRenderer?,
)

@Serializable
data class AccountSwitcherMultiPageMenuRenderer(
    val footer: AccountSwitcherFooter?,
    val header: AccountSwitcherHeader?,
    val sections: List<AccountSwitcherSection?>?,
    val style: String?,
)

@Serializable
data class AccountSwitcherFooter(
    val multiPageMenuSectionRenderer: AccountSwitcherMultiPageMenuSectionRenderer?,
)

@Serializable
data class AccountSwitcherMultiPageMenuSectionRenderer(
    val items: List<AccountSwitcherItem?>?,
)

@Serializable
data class AccountSwitcherItem(
    val compactLinkRenderer: AccountSwitcherCompactLinkRenderer?,
)

@Serializable
data class AccountSwitcherCompactLinkRenderer(
    val icon: AccountSwitcherIcon?,
    val navigationEndpoint: AccountSwitcherNavigationEndpoint?,
    val style: String?,
    val title: AccountSwitcherTitle?,
)

@Serializable
data class AccountSwitcherIcon(
    val iconType: String?,
)

@Serializable
data class AccountSwitcherNavigationEndpoint(
    val signOutEndpoint: AccountSwitcherSignOutEndpoint?,
    val urlEndpoint: AccountSwitcherUrlEndpoint?,
)

@Serializable
data class AccountSwitcherSignOutEndpoint(
    val hack: Boolean?,
)

@Serializable
data class AccountSwitcherUrlEndpoint(
    val url: String?,
)

@Serializable
data class AccountSwitcherTitle(
    val runs: List<Run?>?,
)

@Serializable
data class AccountSwitcherHeader(
    val simpleMenuHeaderRenderer: AccountSwitcherSimpleMenuHeaderRenderer?,
    val accountsDialogHeaderRenderer: AccountSwitcherAccountsDialogHeaderRenderer?,
    val googleAccountHeaderRenderer: AccountSwitcherGoogleAccountHeaderRenderer?,
    val accountItemSectionHeaderRenderer: AccountSwitcherAccountItemSectionHeaderRenderer?,
)

@Serializable
data class AccountSwitcherSimpleMenuHeaderRenderer(
    val backButton: AccountSwitcherBackButton?,
    val title: AccountSwitcherTitle?,
)

@Serializable
data class AccountSwitcherBackButton(
    val buttonRenderer: AccountSwitcherButtonRenderer?,
)

@Serializable
data class AccountSwitcherButtonRenderer(
    val accessibility: AccountSwitcherAccessibility?,
    val accessibilityData: AccountSwitcherAccessibilityData?,
    val icon: AccountSwitcherIcon?,
    val isDisabled: Boolean?,
    val size: String?,
    val style: String?,
)

@Serializable
data class AccountSwitcherAccessibility(
    val label: String?,
)

@Serializable
data class AccountSwitcherAccessibilityData(
    val accessibilityData: AccountSwitcherAccessibilityDataInner?,
)

@Serializable
data class AccountSwitcherAccessibilityDataInner(
    val label: String?,
)

@Serializable
data class AccountSwitcherSection(
    val accountSectionListRenderer: AccountSwitcherAccountSectionListRenderer?,
)

@Serializable
data class AccountSwitcherAccountSectionListRenderer(
    val contents: List<AccountSwitcherContent?>?,
    val header: AccountSwitcherHeader?,
)

@Serializable
data class AccountSwitcherContent(
    val accountItemSectionRenderer: AccountSwitcherAccountItemSectionRenderer?,
    val accountItem: AccountSwitcherAccountItem?,
)

@Serializable
data class AccountSwitcherAccountItemSectionRenderer(
    val contents: List<AccountSwitcherContent?>?,
    val header: AccountSwitcherHeader?,
)

@Serializable
data class AccountSwitcherAccountItem(
    val onBehalfOfParameter: String?,
    val accountByline: AccountSwitcherAccountByline?,
    val accountLogDirectiveInts: List<Int?>?,
    val accountName: AccountSwitcherAccountName?,
    val accountPhoto: AccountSwitcherAccountPhoto?,
    val channelHandle: AccountSwitcherChannelHandle?,
    val hasChannel: Boolean?,
    val isDisabled: Boolean?,
    val isSelected: Boolean?,
    val mobileBanner: AccountSwitcherMobileBanner?,
    val serviceEndpoint: AccountSwitcherServiceEndpoint?,
    val unlimitedStatus: List<AccountSwitcherUnlimitedStatus?>?,
) {
    fun toAccountInfo(email: String): AccountInfo? {
        return AccountInfo(
            name = accountName?.simpleText ?: return null,
            email = email,
            pageId =
                onBehalfOfParameter
                    ?: serviceEndpoint
                        ?.selectActiveIdentityEndpoint
                        ?.supportedTokens
                        ?.firstOrNull { it?.pageIdToken != null }
                        ?.pageIdToken
                        ?.pageId,
            thumbnails = accountPhoto?.thumbnails?.filterNotNull() ?: emptyList(),
        )
    }
}

@Serializable
data class AccountSwitcherAccountByline(
    val simpleText: String,
)

@Serializable
data class AccountSwitcherAccountName(
    val simpleText: String,
)

@Serializable
data class AccountSwitcherAccountPhoto(
    val thumbnails: List<Thumbnail?>?,
)

@Serializable
data class AccountSwitcherChannelHandle(
    val simpleText: String,
)

@Serializable
data class AccountSwitcherMobileBanner(
    val thumbnails: List<Thumbnail?>?,
)

@Serializable
data class AccountSwitcherServiceEndpoint(
    val selectActiveIdentityEndpoint: SelectActiveIdentityEndpoint?,
)

@Serializable
data class AccountSwitcherUnlimitedStatus(
    val runs: List<Run?>?,
)

@Serializable
data class AccountSwitcherAccountItemSectionHeaderRenderer(
    val title: AccountSwitcherTitle?,
)

@Serializable
data class AccountSwitcherAccountsDialogHeaderRenderer(
    val text: AccountSwitcherText?,
)

@Serializable
data class AccountSwitcherText(
    val runs: List<Run?>?,
)

@Serializable
data class AccountSwitcherGoogleAccountHeaderRenderer(
    val email: AccountSwitcherEmail?,
    val name: AccountSwitcherName?,
)

@Serializable
data class AccountSwitcherEmail(
    val runs: List<Run?>?,
)

@Serializable
data class AccountSwitcherName(
    val runs: List<Run?>?,
)

@Serializable
data class AccountSwitcherResponseContext(
    val serviceTrackingParams: List<AccountSwitcherServiceTrackingParam?>?,
)

@Serializable
data class AccountSwitcherServiceTrackingParam(
    val params: List<AccountSwitcherParam?>?,
    val service: String?,
)

@Serializable
data class AccountSwitcherParam(
    val key: String?,
    val value: String?,
)

@Serializable
data class AccountSwitcherSelectText(
    val runs: List<Run?>?,
)

@Serializable
data class SelectActiveIdentityEndpoint(
    val supportedTokens: List<SupportedToken?>?,
) {
    @Serializable
    data class SupportedToken(
        val accountSigninToken: AccountSigninToken?,
        val accountStateToken: AccountStateToken?,
        val datasyncIdToken: DatasyncIdToken?,
        val offlineCacheKeyToken: OfflineCacheKeyToken?,
        val pageIdToken: PageIdToken?,
    ) {
        @Serializable
        data class AccountSigninToken(
            val signinUrl: String?,
        )

        @Serializable
        data class AccountStateToken(
            val hasChannel: Boolean?,
            val isMerged: Boolean?,
            val obfuscatedGaiaId: String?,
        )

        @Serializable
        data class DatasyncIdToken(
            val datasyncIdToken: String?,
        )

        @Serializable
        data class OfflineCacheKeyToken(
            val clientCacheKey: String?,
        )

        @Serializable
        data class PageIdToken(
            val pageId: String?,
        )
    }
}

fun AccountSwitcherEndpointResponse.toListAccountInfo(): List<AccountInfo> {
    if (this.code == "SUCCESS" && this.data != null) {
        val list = mutableListOf<AccountInfo>()
        this.data.contents
            ?.firstOrNull()
            ?.accountSectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.accountItemSectionRenderer
            ?.contents
            ?.forEach { content ->
                content?.accountItem?.let { accountItem ->
                    accountItem
                        .toAccountInfo(
                            email =
                                accountItem.channelHandle
                                    ?.simpleText ?: "",
                        )?.let {
                            list.add(it)
                        }
                }
            }
        return list
    } else {
        return emptyList()
    }
}
