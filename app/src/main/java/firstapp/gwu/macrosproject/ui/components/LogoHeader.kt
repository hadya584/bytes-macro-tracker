package firstapp.gwu.macrosproject.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import firstapp.gwu.macrosproject.R

@Composable
fun AppLogoHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),  // adjust height for how tall you want the header
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.bytes_logo),
            contentDescription = "Bytes Logo",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentScale = ContentScale.Fit   // or try ContentScale.FillWidth for a banner effect
        )
    }
}
