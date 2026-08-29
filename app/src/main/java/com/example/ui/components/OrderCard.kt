package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FurnitureOrder
import com.example.util.PersianUtils

private val CardCellDarkBg = Color(0xFF0F172A).copy(alpha = 0.78f)
private val CardCellBorder = Color.White.copy(alpha = 0.15f)
private val CardGoldBorder = Color(0xFFD4A017)
private val LabelGrayText = Color(0xFFCBD5E1)
private val ValueWhiteText = Color(0xFFFFFFFF)
private val EditButtonGreen = Color(0xFF107C41)
private val DeleteButtonRed = Color(0xFFA82828)
private val ShareButtonBlue = Color(0xFF1D4ED8)

@Composable
fun OrderCard(
    order: FurnitureOrder,
    currencyUnit: String,
    onEdit: (FurnitureOrder) -> Unit,
    onDelete: (FurnitureOrder) -> Unit,
    onViewInvoice: (FurnitureOrder) -> Unit,
    onCustomerClick: (String) -> Unit,
    onModelClick: (String) -> Unit,
    onDuplicate: ((FurnitureOrder) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val modelColor = PersianUtils.parseColor(
        if (order.colorCode.isNotBlank()) order.colorCode else PersianUtils.getModelColor(order.modelName)
    )

    Surface(
        color = modelColor,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(2.5.dp, CardGoldBorder),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TOP HEADER: Spacing + Row Number ("شماره ردیف")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Row Number Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "شماره ردیف: ${PersianUtils.toPersianDigits(order.orderNumber)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Small model color dot/indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                )
            }

            // ROW 1: [مدل مبل] (Right) | [دستمزد هر دست] (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right: مدل
                OrderCell(
                    label = "مدل",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onModelClick(order.modelName) }
                ) {
                    Text(
                        text = order.modelName,
                        color = ValueWhiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Left: دستمزد هر دست
                OrderCell(
                    label = "دستمزد هر دست",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = currencyUnit,
                            color = LabelGrayText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = PersianUtils.formatNumberWithCommas(order.pricePerSet),
                            color = ValueWhiteText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // ROW 2: [تعداد واحد در یک دست] (Right) | [تعداد] (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right: تعداد واحد در یک دست
                OrderCell(
                    label = "تعداد واحد در یک دست",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = PersianUtils.toPersianDigits(order.unitsPerSet),
                        color = ValueWhiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Left: تعداد
                OrderCell(
                    label = "تعداد",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = PersianUtils.toPersianDigits(order.countFormula),
                        color = ValueWhiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ROW 3: [مجموع واحد] (Right) | [دستمزد] (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right: مجموع واحد
                OrderCell(
                    label = "مجموع واحد",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = PersianUtils.formatNumberWithCommas(order.calculatedUnits),
                        color = ValueWhiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Left: دستمزد
                OrderCell(
                    label = "دستمزد",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = currencyUnit,
                            color = LabelGrayText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = PersianUtils.formatNumberWithCommas(order.calculatedTotal),
                            color = ValueWhiteText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // ROW 4: [تاریخ] (Right) | [شماره فاکتور] (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right: تاریخ
                OrderCell(
                    label = "تاریخ",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = PersianUtils.toPersianDigits(order.dateJalali),
                        color = ValueWhiteText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Left: شماره فاکتور
                OrderCell(
                    label = "شماره فاکتور",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = PersianUtils.toPersianDigits(order.invoiceNumber),
                        color = ValueWhiteText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ROW 5: [مشتری / نمایشگاه] (Right) | [توضیحات] (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right: مشتری / نمایشگاه
                OrderCell(
                    label = "مشتری / نمایشگاه",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onCustomerClick(order.customerName) }
                ) {
                    Text(
                        text = order.customerName.ifBlank { "مشتری عمومی" },
                        color = ValueWhiteText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Left: توضیحات (شامل نام و مشخصات پارچه در ابتدا و سپس متن توضیحات)
                OrderCell(
                    label = "توضیحات",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val fullNotes = buildString {
                        if (order.fabricName.isNotBlank()) {
                            append("پارچه: ${order.fabricName}")
                        }
                        if (order.notes.isNotBlank()) {
                            if (order.fabricName.isNotBlank()) append(" - ")
                            append(order.notes)
                        }
                    }.ifBlank { "-" }

                    Text(
                        text = fullNotes,
                        color = ValueWhiteText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // BOTTOM ACTION BUTTONS: [حذف] (Red) & [ویرایش] (Green) & [اشتراک گذاری] (Blue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // حذف Button (Red)
                    Button(
                        onClick = { onDelete(order) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeleteButtonRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "حذف",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ویرایش Button (Green)
                    Button(
                        onClick = { onEdit(order) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EditButtonGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "ویرایش",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // اشتراک گذاری Button (Blue)
                    Button(
                        onClick = { onViewInvoice(order) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShareButtonBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "اشتراک گذاری",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "اشتراک گذاری",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCell(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = CardCellDarkBg,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardCellBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = LabelGrayText,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            content()
        }
    }
}
