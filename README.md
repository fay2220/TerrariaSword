# Starfury (Forge 1.20.1)

ดาบ Starfury จาก Terraria สำหรับ Minecraft Forge 1.20.1 (Forge 47.x, Java 17)

## การใช้งาน
| ปุ่ม | ผลลัพธ์ |
|------|---------|
| คลิกซ้าย | ตีธรรมดา ดาเมจ 7 (1 ฐาน + 3 tier + 3 modifier) |
| คลิกขวา | เสกดาวห้าแฉก 3–5 ดวงตกลงมาที่จุดที่ crosshair ชี้ (ชี้ที่มอนสเตอร์ = ตกใส่ตัวมอน) ดวงละ 5 ดาเมจ |

### เอฟเฟกต์ดาวตก
- ฟ้าแวบ (flash) ตรงจุดที่ดาวแต่ละดวงปรากฏ และวงแสงบนพื้นตรงจุดที่ crosshair ชี้
- ดาวสีทองดวงใหญ่ มีหางประกายไฟตามหลัง ตกลงมาไม่พร้อมกัน (ความสูงต่างกัน)
- ตอนกระแทก: แสงวาบ, ระเบิดประกายทรงกลม, คลื่นกระแทก 3 ชั้นตามพื้น, เสาแสงพุ่งขึ้นฟ้า, เสียงระเบิด
- มอนสเตอร์ทุกตัวในรัศมี 2.5 บล็อกรอบจุดตกโดนดาเมจ 5 พร้อมถูกกระเด็น (ไม่โดนตัวผู้เล่นเอง)
- หน้าจอสั่นตามระยะห่างจากจุดตก
- ตัวดาวเป็นโมเดล 3 มิติของ GeckoLib หมุนตีลังกาและเต้นเป็นจังหวะ เรืองแสงตลอดแม้ในที่มืด

## ต้องมี GeckoLib ด้วย
mod นี้ต้องใช้ **GeckoLib** (เวอร์ชัน 4.7 ขึ้นไป สำหรับ Forge 1.20.1) โหลดได้ที่ CurseForge หรือ Modrinth (ค้นหา "GeckoLib")
แล้วใส่ไฟล์ `geckolib-forge-1.20.1-4.x.x.jar` ในโฟลเดอร์ `mods` คู่กับ `starfury-1.0.0.jar`
ถ้าลืมใส่ เกมจะขึ้นข้อความแจ้งว่าขาด GeckoLib

ไอเท็มอยู่ในแท็บ Combat (หรือ `/give @s starfury:starfury`)

## วิธีได้ไฟล์ .jar แบบง่ายที่สุด (ไม่ต้องติดตั้งอะไร)
1. สมัคร/ล็อกอิน GitHub แล้วสร้าง repository ใหม่ (Private ก็ได้)
2. อัปโหลดไฟล์ทั้งหมดในโฟลเดอร์นี้ขึ้นไป (รวมโฟลเดอร์ `.github`)
3. เปิดแท็บ **Actions** รอประมาณ 3–5 นาทีจน build เสร็จ (เครื่องหมายถูกสีเขียว)
4. คลิกที่ run นั้น เลื่อนลงไปหัวข้อ **Artifacts** โหลด `starfury-jar` แล้วแตก zip จะได้ `starfury-1.0.0.jar`
5. ใส่ไฟล์ .jar ในโฟลเดอร์ `mods` ของ Minecraft Forge 1.20.1 ได้เลย

## วิธี build เอง
1. ติดตั้ง JDK 17
2. ในโฟลเดอร์นี้ ให้มี Gradle wrapper (ถ้ายังไม่มี ให้คัดลอก `gradlew`, `gradlew.bat`, โฟลเดอร์ `gradle/` จาก Forge MDK 1.20.1 ที่ https://files.minecraftforge.net หรือรัน `gradle wrapper` ถ้าติดตั้ง Gradle ไว้)
3. รัน `./gradlew build` (Windows: `gradlew build`)
4. ไฟล์ mod จะอยู่ที่ `build/libs/starfury-1.0.0.jar` → ใส่ในโฟลเดอร์ `mods` ของ Forge 1.20.1
5. ทดสอบในเกมโดยตรง: `./gradlew runClient`

เปิดใน IntelliJ / Eclipse ได้โดย import เป็นโปรเจกต์ Gradle

## ปรับค่าต่างๆ
ใน `StarfurySwordItem.java`
- `COOLDOWN_TICKS` คูลดาวน์คลิกขวา (20 tick = 1 วินาที)
- `RANGE` ระยะสูงสุดที่ crosshair เล็งได้
- `SPAWN_HEIGHT_*`, `SPAWN_SPREAD`, `LANDING_SPREAD` ความสูงและการกระจายของดาว
- จำนวนดาว: `3 + rnd.nextInt(3)` (3–5 ดวง)

ใน `FallenStarEntity.java` : `DAMAGE` (ดาเมจดาวต่อดวง = 5), `SPLASH_RADIUS` (รัศมีระเบิด; ตั้งเป็น 0 ถ้าอยากให้โดนเฉพาะตัวที่ดาวชนตรงๆ)
ใน `ClientEffects.java` : `MAX_SHAKE` / `SHAKE_RANGE` ความแรงและระยะของหน้าจอสั่น, จำนวน particle ของการระเบิด
ใน `animations/entity/fallen_star.animation.json` : ความเร็วหมุนและขนาดดาว (ค่า scale 1.8–2.3)
ใน `gradle.properties` : `geckolib_version` (เวอร์ชัน GeckoLib ที่ใช้ตอน build)
ใน `ModItems.java` : ดาเมจ/ความเร็วตีของดาบ
