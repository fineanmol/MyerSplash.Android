package com.juniperphoton.myersplash.compose.about

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.juniperphoton.myersplash.R
import com.juniperphoton.myersplash.activity.BaseActivity
import com.juniperphoton.myersplash.compose.theme.AppTheme
import com.juniperphoton.myersplash.extension.getVersionName
import com.juniperphoton.myersplash.extension.startActivitySafely
import com.juniperphoton.myersplash.viewmodel.BaseViewModel

/**
 * @author dengweichao @ Zhihu Inc.
 * @since 2021-03-07
 */
class AboutComposeActivity : BaseActivity() {
    private val viewModel: AboutViewModel by lazy {
        ViewModelProvider(this).get(AboutViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    AboutContent(viewModel)
                }
            }
        }
    }
}

interface IAboutViewModel {
    fun handleFeedback(kind: FeedbackKind)
    fun handleRate()
}

class AboutViewModel(application: Application) : BaseViewModel(application), IAboutViewModel {
    override fun handleFeedback(kind: FeedbackKind) {
        when (kind) {
            FeedbackKind.Twitter -> {
                onClickUrl(app.getString(R.string.twitter_url))
            }
            FeedbackKind.GitHub -> {
                onClickUrl(app.getString(R.string.github_url))
            }
            FeedbackKind.Email -> {
                onClickEmail()
            }
        }
    }

    override fun handleRate() {
        val uri = Uri.parse("market://details?id=${app.packageName}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        app.startActivitySafely(intent)
    }

    private fun onClickEmail() {
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "message/rfc822"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.email_url)))

        emailIntent.putExtra(
            Intent.EXTRA_SUBJECT,
            "MyerSplash for Android ${app.getVersionName()} feedback"
        )
        emailIntent.putExtra(Intent.EXTRA_TEXT, "")

        app.startActivitySafely(Intent.createChooser(emailIntent, getString(R.string.email_title)))
    }

    private fun onClickUrl(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        app.startActivitySafely(intent)
    }
}

@Composable
fun AboutContent(viewModel: IAboutViewModel) {
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(), contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandTitle()
            BrandSubTitle()
            VersionCode()
            SubTitle(R.string.credit)
            Text(
                text = stringResource(id = R.string.settings_credit_content),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 30.dp)
            )
            SubTitle(R.string.feedback)
            FeedbackButtons(onClick = {
                viewModel.handleFeedback(it)
            })
            SubTitle(textRes = R.string.like_this_app)
            RateButton(onClick = {
                viewModel.handleRate()
            })
        }
    }
}

@Composable
fun BrandTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.icon_round),
            contentDescription = "logo",
            modifier = Modifier
                .width(30.dp)
                .height(30.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(id = R.string.myer),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(id = R.string.splash),
            fontSize = 40.sp,
        )
    }
}

@Composable
fun BrandSubTitle() {
    Column {
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(id = R.string.for_windows_amp_android), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun VersionCode() {
    Box(
        modifier = Modifier
            .background(
                color = colorResource(id = R.color.MyerSplashThemeColor),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = LocalContext.current.getVersionName() ?: "Version 1.1.0",
            fontSize = 12.sp,
            color = Color.White
        )
    }
}

@Composable
fun SubTitle(textRes: Int) {
    Column {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = textRes),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = colorResource(
                id = R.color.MyerSplashThemeColor
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

enum class FeedbackKind {
    Email,
    Twitter,
    GitHub
}

@Composable
fun FeedbackButtons(onClick: (FeedbackKind) -> Unit) {
    Row {
        FeedbackKind.values().forEach {
            FeedbackButton(kind = it, onClick = onClick)
        }
    }
}

@Composable
fun FeedbackButton(kind: FeedbackKind, onClick: (FeedbackKind) -> Unit) {
    val iconRes = when (kind) {
        FeedbackKind.Email -> R.drawable.ic_email
        FeedbackKind.GitHub -> R.drawable.ic_github_icon
        FeedbackKind.Twitter -> R.drawable.ic_twitter
    }
    val desc = when (kind) {
        FeedbackKind.Email -> "email"
        FeedbackKind.GitHub -> "github"
        FeedbackKind.Twitter -> "twitter"
    }
    Image(
        painter = painterResource(id = iconRes),
        contentDescription = desc,
        colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                onClick(kind)
            }
            .size(dimensionResource(id = R.dimen.about_contact_item_size))
            .padding(12.dp))
}

@Composable
fun RateButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.give_me_a_rate),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Preview
@Composable
fun AboutContentPreview() {
    val vm = object : IAboutViewModel {
        override fun handleFeedback(kind: FeedbackKind) = Unit
        override fun handleRate() = Unit
    }
    AboutContent(viewModel = vm)
}