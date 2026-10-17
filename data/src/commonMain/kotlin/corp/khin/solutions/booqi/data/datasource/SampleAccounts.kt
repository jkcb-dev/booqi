package corp.khin.solutions.booqi.data.datasource

import corp.khin.solutions.booqi.data.dto.UserDto

/** The sample User that owns the catalog profile [profileId] (nobody can sign in to it). */
internal fun sampleOwnerOf(profileId: String) = "user-of-$profileId"

/** The accounts of [SampleData.accounts]: the demo account, the sample Customers, the owners. */
internal fun sampleAccounts(): List<SeedAccount> = listOf(
    SeedAccount(
        user = verifiedAccount(SampleData.DEMO_USER_ID, "Demo Booqi", SampleData.DEMO_EMAIL),
        password = SampleData.DEMO_PASSWORD,
    ),
    SeedAccount(verifiedAccount(SampleData.SAMPLE_CUSTOMER_ID, "Camila Torres", "camila@booqi.test")),
    SeedAccount(
        UserDto(
            id = SampleData.DELETED_CUSTOMER_ID,
            displayName = "",
            email = null,
            photoUrl = null,
            isEmailVerified = false,
            socialProvider = null,
            isDeleted = true,
        ),
    ),
    SeedAccount(verifiedAccount(sampleOwnerOf(SampleData.SAMPLE_PROFILE_ID), "Lucía Fernández", "lucia@booqi.test")),
    SeedAccount(verifiedAccount(sampleOwnerOf(SampleData.SAMPLE_PROFILE_2_ID), "Martín Gómez", "martin@booqi.test")),
)

private fun verifiedAccount(id: String, name: String, email: String) = UserDto(
    id = id,
    displayName = name,
    email = email,
    photoUrl = null,
    isEmailVerified = true,
    socialProvider = null,
    isDeleted = false,
)
