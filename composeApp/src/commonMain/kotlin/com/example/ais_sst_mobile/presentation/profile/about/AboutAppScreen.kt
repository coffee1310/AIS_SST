package com.example.ais_sst_mobile.presentation.profile.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ais_sst_mobile.navigation.AboutAppComponent
import com.example.ais_sst_mobile.presentation.components.AppBackground
import com.example.ais_sst_mobile.presentation.components.CustomBackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(component: AboutAppComponent) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        ) {
            // Верхняя панель
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomBackButton(onClick = component.onGoBack)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "О приложении",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(40.dp))
            }

            // Контент
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Мобильное приложение Студенческого совета",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = """
Мобильное приложение Студенческого совета разработано с целью повышения вовлечённости студентов в общественную деятельность и упрощения взаимодействия с организацией мероприятий.

Приложение предоставляет возможность поиска и регистрации на мероприятия, управления заявками на роли, ведения личного календаря, а также отслеживания достижений и рейтинга.

Структура приложения включает следующие разделы:

Главная. На данном экране отображаются ближайшие мероприятия, а также мероприятия, доступные для регистрации на роли. Реализована функция поиска по названию события.

Заявки. Раздел предназначен для отслеживания статуса поданных заявок. Заявки распределены по категориям: на рассмотрении, принятые, отклонённые и резерв. В разделе принятых заявок доступна информация о дедлайнах и возможность отказа от участия.

Календарь. Экран позволяет визуально отслеживать даты проведения мероприятий, в которых пользователь принимает участие, и планировать свою загруженность.

Сектора. Раздел содержит информацию обо всех структурных подразделениях Студенческого совета. Пользователь может ознакомиться с деятельностью секторов, их координаторами и текущим составом участников.

Профиль. В личном кабинете отображаются данные пользователя, текущий рейтинг и количество баллов. Доступна возможность редактирования личной информации и управления настройками учётной записи.

Приложение ориентировано на активных студентов и призвано способствовать развитию студенческого самоуправления.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight.value * 1.3f).sp
                        )
                    }
                }
            }
        }
    }
}