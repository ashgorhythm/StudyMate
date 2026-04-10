package com.example.myandroidapp.ui.screens.community

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myandroidapp.data.model.*
import com.example.myandroidapp.ui.theme.*

// ─────────────────────────────────────────────────────────
// Communities Tab — List of communities with join/create
// ─────────────────────────────────────────────────────────

@Composable
fun CommunitiesTab(
    communities: List<CommunityInfo>,
    currentMemberId: String,
    onJoin: (String) -> Unit,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onManageMembers: (String) -> Unit,
    onOpenGroupDetail: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search communities...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.15f),
                cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
            )
        )
        Spacer(Modifier.height(16.dp))

        // Create community button
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onCreate() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(0.08f)),
            border = BorderStroke(1.dp, TealPrimary.copy(0.2f))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(TealPrimary.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Add, null, tint = TealPrimary, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Create Community", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                    Text("Start a new study group", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("All Communities", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(12.dp))

        val filtered = if (searchQuery.isBlank()) communities
        else communities.filter { it.entity.name.contains(searchQuery, true) || it.entity.description.contains(searchQuery, true) }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No communities found", color = TextMuted, fontSize = 14.sp)
            }
        }

        filtered.forEach { info ->
            CommunityListItem(
                info = info,
                currentMemberId = currentMemberId,
                onJoin = { onJoin(info.entity.communityId) },
                onSelect = { onSelect(info.entity.communityId) },
                onManage = { onManageMembers(info.entity.communityId) },
                onNameClick = { onOpenGroupDetail(info.entity.communityId) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CommunityListItem(
    info: CommunityInfo,
    currentMemberId: String,
    onJoin: () -> Unit,
    onSelect: () -> Unit,
    onManage: () -> Unit,
    onNameClick: () -> Unit = {}
) {
    val c = info.entity
    val isMember = info.membershipStatus == MembershipStatus.APPROVED
    val isPending = info.membershipStatus == MembershipStatus.PENDING
    val isAdmin = info.memberRole == CommunityRole.ADMIN
    val isMod = info.memberRole == CommunityRole.MODERATOR

    Card(
        modifier = Modifier.fillMaxWidth().clickable { if (isMember) onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, if (isMember) TealPrimary.copy(0.15f) else TextMuted.copy(0.08f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(TealPrimary.copy(0.15f), PurpleAccent.copy(0.1f)))),
                    contentAlignment = Alignment.Center
                ) { Text(c.iconEmoji, fontSize = 24.sp) }

                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            c.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, false)
                                .clickable(onClick = onNameClick)
                        )
                        Spacer(Modifier.width(6.dp))
                        if (!c.isPublic) Icon(Icons.Default.Lock, null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                    }
                    Text("${c.memberCount} members · ID: ${c.communityId}", fontSize = 11.sp, color = TextMuted)
                }

                // Action button
                when {
                    isPending -> {
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(AmberAccent.copy(0.15f)).padding(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text("Pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAccent) }
                    }
                    isMember -> {
                        Row {
                            if (isAdmin || isMod) {
                                IconButton(onClick = onManage, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ManageAccounts, null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp)).background(GreenSuccess.copy(0.15f)).padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { Text("Joined", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenSuccess) }
                        }
                    }
                    else -> {
                        Button(
                            onClick = onJoin,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) { Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            if (c.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(c.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
            }

            // Role badge
            if (isMember && (isAdmin || isMod)) {
                Spacer(Modifier.height(6.dp))
                val roleColor = if (isAdmin) AmberAccent else PurpleAccent
                val roleLabel = if (isAdmin) "Admin" else "Moderator"
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(roleColor.copy(0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)
                ) { Text("⭐ $roleLabel", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = roleColor) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Create Community Dialog
// ─────────────────────────────────────────────────────────

@Composable
fun CreateCommunityDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, isPublic: Boolean, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var emoji by remember { mutableStateOf("📚") }
    val emojis = listOf("📚", "💻", "🧮", "🔬", "⚔️", "🎨", "🎵", "🏆", "🌍", "🧠")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Create Community", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Emoji selector
                Text("Icon", fontSize = 13.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emojis.forEach { e ->
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(if (emoji == e) TealPrimary.copy(0.2f) else Color.Transparent)
                                .border(if (emoji == e) 1.5.dp else 0.dp, if (emoji == e) TealPrimary else Color.Transparent, CircleShape)
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 18.sp) }
                    }
                }

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    placeholder = { Text("Community name", color = TextMuted) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.2f), cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    placeholder = { Text("Description (optional)", color = TextMuted) },
                    minLines = 2, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TealPrimary, unfocusedBorderColor = TextMuted.copy(0.2f), cursorColor = TealPrimary, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                // Privacy toggle
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(if (isPublic) "Public Community" else "Private Community", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(
                            if (isPublic) "Anyone can join" else "Requires admin approval",
                            fontSize = 11.sp, color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isPublic, onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NavyDark, checkedTrackColor = TealPrimary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim(), description.trim(), isPublic, emoji) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(TealPrimary, NavyDark)
            ) { Text("Create", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } }
    )
}

// ─────────────────────────────────────────────────────────
// Manage Members Dialog — Intuitive member request management
// ─────────────────────────────────────────────────────────

@Composable
fun ManageMembersDialog(
    communityName: String,
    pendingRequests: List<CommunityMemberEntity>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyMedium,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(PurpleAccent.copy(0.2f), TealPrimary.copy(0.15f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ManageAccounts, null, tint = PurpleAccent, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Member Requests", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(communityName, color = TextMuted, fontSize = 12.sp)
                    }
                }
                if (pendingRequests.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    // Request count badge
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AmberAccent.copy(0.1f), AmberAccent.copy(0.05f))
                                )
                            )
                            .border(1.dp, AmberAccent.copy(0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${pendingRequests.size} pending request${if (pendingRequests.size > 1) "s" else ""} awaiting review",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberAccent
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pendingRequests.isEmpty()) {
                    // Empty state with illustration
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(GreenSuccess.copy(0.15f), GreenSuccess.copy(0.03f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✅", fontSize = 36.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "All caught up!",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "No pending requests at the moment",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    pendingRequests.forEach { member ->
                        // Enhanced member request card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(SurfaceCard),
                            border = BorderStroke(1.dp, PurpleAccent.copy(0.1f))
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                // Profile row
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Gradient avatar
                                    Box(
                                        Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        PurpleAccent.copy(0.3f),
                                                        TealPrimary.copy(0.2f)
                                                    )
                                                )
                                            )
                                            .border(1.5.dp, PurpleAccent.copy(0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            member.displayName.take(1).uppercase(),
                                            fontSize = 20.sp,
                                            color = PurpleAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            member.displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Schedule, null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Requested to join",
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    // Pending badge
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AmberAccent.copy(0.1f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            "PENDING",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberAccent,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Action buttons row (full width, obvious)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Reject button
                                    OutlinedButton(
                                        onClick = { onReject(member.memberId) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, RedError.copy(0.3f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = RedError
                                        ),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close, null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Decline",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Approve button
                                    Button(
                                        onClick = { onApprove(member.memberId) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GreenSuccess,
                                            contentColor = NavyDark
                                        ),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Check, null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Approve",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TealPrimary.copy(0.08f))
            ) {
                Text("Done", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    )
}
