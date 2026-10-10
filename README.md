# Starfury (Forge 1.20.1)

ดาบจาก Terraria สำหรับ Minecraft Forge 1.20.1 (Forge 47.x, Java 17): **Starfury** และ **Zenith Sword**
เวอร์ชัน mod: 1.1.0

## ไฟล์ที่ต้องใส่ในโฟลเดอร์ `mods`
| ไฟล์ | หมายเหตุ |
|------|---------|
| `starfury-1.1.0.jar` | mod นี้ (ได้จาก build ดูด้านล่าง) ลบ `starfury-1.0.0.jar` ตัวเก่าออกก่อน |
| `geckolib-forge-1.20.1-4.7.4.jar` (หรือ 4.7.x / 4.8.x ที่ใหม่กว่า) | โหลดที่ CurseForge หรือ Modrinth ค้นหา "GeckoLib" เลือก Forge 1.20.1 |

ไม่ต้องใช้ library อื่นเพิ่ม (Zenith ไม่ได้ใช้ library ใหม่) ถ้าลืมใส่ GeckoLib เกมจะขึ้นข้อความแจ้งว่าขาด GeckoLib

ดาบทั้งสองเล่มอยู่ในแท็บ Combat หรือใช้คำสั่ง `/give @s starfury:starfury` และ `/give @s starfury:zenith_sword`
(ยังไม่มีสูตร craft หรือ loot)

## Starfury
| ปุ่ม | ผลลัพธ์ |
|------|---------|
| คลิกซ้าย | ตีธรรมดา ดาเมจ 7 (1 ฐาน + 3 tier + 3 modifier) |
| คลิกขวา | เสกดาวห้าแฉก 3–5 ดวงตกลงมาที่จุดที่ crosshair ชี้ (ชี้ที่มอนสเตอร์ = ตกใส่ตัวมอน) ดวงละ 5 ดาเมจ, คูลดาวน์ 1 วินาที |

เอฟเฟกต์: ฟ้าแวบตรงจุดที่ดาวปรากฏ, ดาว 3 มิติ (GeckoLib) หมุนตีลังกา เรืองแสง มีหางประกายไฟ, ตอนกระแทกมีแสงวาบ ระเบิดประกาย คลื่นกระแทก 3 ชั้น เสาแสง เสียงระเบิด และหน้าจอสั่น
มอนในรัศมี 2.5 บล็อกรอบจุดตกโดน 5 ดาเมจ พร้อมกระเด็น (ไม่โดนตัวผู้เล่นเอง)

## Zenith Sword (ใหม่ใน 1.1.0)
| ปุ่ม | ผลลัพธ์ |
|------|---------|
| คลิกซ้าย | ตีธรรมดา ดาเมจ 25 (1 ฐาน + 4 tier Netherite + 20 modifier), ความเร็วตี 2.0 ครั้ง/วินาที |
| กดคลิกขวาค้าง | เสกดาบวิญญาณ 5 เล่ม/วินาที บินวนเป็นวงไป-กลับไกลสุด 20 บล็อกตามทิศที่หันหน้า แต่ละเล่มใช้เวลาไป-กลับ 1 วินาที ฟันทุกตัวที่ขวางทาง เล่มละ 20 ดาเมจ (มอนตัวเดียวโดนประมาณ 5 ครั้ง/วินาที) ไม่มีคูลดาวน์ ใช้ได้เรื่อยๆ จนกว่าจะปล่อยคลิกขวา |

รายละเอียด
- ดาบไม่ทะลุบล็อก: ถ้าชนกำแพงจะเกิดประกายไฟ เสียงกริ๊ง แล้วบินกลับมาหาผู้เล่นทันที ระยะ 20 บล็อกก็สั้นลงเองถ้ามีกำแพงอยู่ใกล้กว่า
- ดาบแต่ละเล่มสุ่มหน้าตาจาก 6 แบบ: Zenith, Starfury และดาบวิญญาณ 4 สี (มรกต, ส้มไฟ, ชมพู, ฟ้า) เล่มแรกของทุกครั้งเป็น Zenith และพุ่งตรงไปที่ crosshair พอดี
- ไม่โดนตัวเอง สัตว์เลี้ยงของตัวเอง และเพื่อนร่วมทีม (scoreboard team)
- เดินและวิ่งได้ความเร็วปกติระหว่างกดค้าง (ปกติ Minecraft จะลดความเร็วตอนใช้ไอเท็ม)
- ไม่เสียความทนทานตอนใช้คลิกขวา (เสียเฉพาะตอนตีด้วยคลิกซ้ายแบบดาบปกติ) และไม่ติดไฟ (เหมือนของ Netherite)

### เอฟเฟกต์ของ Zenith
- ตัวดาบเรืองแสงเต็มที่ มีเงาตามหลัง (afterimage) 4 ชั้นจางลงเรื่อยๆ มีแสงรัศมีสีตามแบบดาบ และริบบิ้นแสงลากตามเส้นทางบิน
- อนุภาคประกายสีตามแบบดาบตลอดเส้นทางบิน
- ทุกครั้งที่ดาบหนึ่งเล่มถึงจุดไกลสุด: sonic boom (วงแหวนแบบ Warden) + วงไอน้ำ + เสียงแตก
- ทุก 2 วินาทีที่กดค้าง (และตอนเริ่มกด): **การทำลายกำแพงเสียง** ทางทิศที่หันหน้า: กรวยไอน้ำซ้อนกัน 4 ชั้น, คลื่นกระแทกขยายออก, เส้น sonic boom พุ่งไปข้างหน้าไกลสุด 14 บล็อก (หยุดที่กำแพง), เสียง sonic boom ของ Warden, หน้าจอสั่น, กล้องซูมกระตุก และเส้นความเร็วแบบอนิเมะบนหน้าจอ
- **Impact frame** ตอนเริ่มกดครั้งแรก: จอขาววาบ, ตามด้วยเฟรมมืด แล้วจางออก (ถ้าแสงกะพริบรบกวน ตั้ง `ENABLE_SCREEN_FLASH = false` ใน `ZenithClientEffects.java`)
- ผู้เล่นคนอื่นที่อยู่ใกล้ก็เห็นเอฟเฟกต์ทั้งหมดและหน้าจอสั่นตามระยะ (เส้นความเร็วกับจอวาบเห็นเฉพาะคนที่ใช้ดาบ)

## วิธีได้ไฟล์ .jar แบบง่ายที่สุด (ไม่ต้องติดตั้งอะไร)
1. สมัคร/ล็อกอิน GitHub แล้วสร้าง repository ใหม่ (Private ก็ได้) หรือใช้ repository เดิม
2. อัปโหลดไฟล์ทั้งหมดในโฟลเดอร์นี้ขึ้นไป (รวมโฟลเดอร์ `.github`) ทับของเดิม
3. เปิดแท็บ **Actions** รอประมาณ 3–5 นาทีจน build เสร็จ (เครื่องหมายถูกสีเขียว)
4. คลิกที่ run นั้น เลื่อนลงไปหัวข้อ **Artifacts** โหลด `starfury-jar` แล้วแตก zip จะได้ `starfury-1.1.0.jar`
5. ใส่ไฟล์ .jar คู่กับ GeckoLib ในโฟลเดอร์ `mods` ของ Minecraft Forge 1.20.1 ได้เลย

ถ้า build ไม่ผ่าน ให้เปิด run ที่มีกากบาทแดง คลิกขั้นตอน "Build mod jar" แล้วคัดลอก log ส่วนที่มีคำว่า `error` มาให้แก้

## วิธี build เอง
1. ติดตั้ง JDK 17
2. ในโฟลเดอร์นี้ ให้มี Gradle wrapper (ถ้ายังไม่มี ให้คัดลอก `gradlew`, `gradlew.bat`, โฟลเดอร์ `gradle/` จาก Forge MDK 1.20.1 ที่ https://files.minecraftforge.net หรือรัน `gradle wrapper --gradle-version 8.1.1` ถ้าติดตั้ง Gradle ไว้)
3. รัน `./gradlew build` (Windows: `gradlew build`)
4. ไฟล์ mod จะอยู่ที่ `build/libs/starfury-1.1.0.jar` → ใส่ในโฟลเดอร์ `mods` ของ Forge 1.20.1
5. ทดสอบในเกมโดยตรง: `./gradlew runClient`

เปิดใน IntelliJ / Eclipse ได้โดย import เป็นโปรเจกต์ Gradle

## ปรับค่าต่างๆ
Starfury
- `StarfurySwordItem.java`: `COOLDOWN_TICKS` (20 tick = 1 วินาที), `RANGE`, `SPAWN_HEIGHT_*`, `SPAWN_SPREAD`, `LANDING_SPREAD`, จำนวนดาว `3 + rnd.nextInt(3)`
- `FallenStarEntity.java`: `DAMAGE` (= 5), `SPLASH_RADIUS` (ตั้งเป็น 0 ถ้าอยากให้โดนเฉพาะตัวที่ชนตรงๆ)
- `ClientEffects.java`: `MAX_SHAKE` / `SHAKE_RANGE` ความแรงและระยะหน้าจอสั่น, จำนวน particle ของการระเบิด
- `animations/entity/fallen_star.animation.json`: ความเร็วหมุนและขนาดดาว (ค่า scale ตอนนี้ 1.0–1.3)

Zenith
- `ModItems.java`: ดาเมจคลิกซ้าย (`20` = modifier, รวมเป็น 25) และความเร็วตี (`-2.0F` → 2.0 ครั้ง/วินาที; `-2.4F` = เท่าดาบปกติ)
- `ZenithSwordItem.java`: `LAUNCH_INTERVAL` (4 tick = 5 เล่ม/วินาที), `BIG_BOOM_INTERVAL` (ทุกกี่ tick จะเกิด sonic boom ใหญ่)
- `ZenithSwordEntity.java`: `DAMAGE` (= 20), `RANGE` (= 20 บล็อก), `FLIGHT_TICKS` (= 20 tick = 1 วินาที), `SPREAD_DEGREES` (ดาบกระจายรอบ crosshair กี่องศา), `VARIANT_COLORS` (สีของแต่ละแบบ)
- `ZenithSwordRenderer.java`: `SCALE` ขนาดดาบที่บิน, `AFTERIMAGES` จำนวนเงา, `RIBBON_WIDTH` ความกว้างริบบิ้นแสง
- `ZenithClientEffects.java`: `ENABLE_SCREEN_FLASH`, `FLASH_TICKS`, `SPEED_LINE_TICKS`, `SPEED_LINES`, `FOV_PUNCH`, `BOOM_LINE_LENGTH`
- `gradle.properties`: `geckolib_version` (เวอร์ชัน GeckoLib ที่ใช้ตอน build), `mod_version`

## โครงสร้างโค้ด
| ไฟล์ | หน้าที่ |
|------|--------|
| `StarfuryMod.java` | จุดเริ่ม mod, ลงทะเบียน item/entity, ใส่ดาบในแท็บ Combat |
| `ModItems.java` / `ModEntities.java` | ลงทะเบียน item และ entity (DeferredRegister) |
| `StarfurySwordItem.java`, `FallenStarEntity.java`, `FallenStarRenderer.java` | Starfury และดาวตก (GeckoLib) |
| `ZenithSwordItem.java` | Zenith: กดคลิกขวาค้าง ปล่อยดาบทุก 4 tick (ฝั่ง server) |
| `ZenithSwordEntity.java` | ดาบที่บิน: เส้นทางวนไป-กลับ, ชนกำแพง, ทำดาเมจ (server) และส่ง entity event ให้ client |
| `ZenithSwordRenderer.java` | วาดดาบ เงา แสงรัศมี ริบบิ้นแสง (client) |
| `ZenithClientEffects.java` | sonic boom, impact frame, เส้นความเร็ว, ซูมกระตุก, แก้การเดินช้าตอนกดค้าง (client) |
| `ClientEffects.java` | เอฟเฟกต์ระเบิดของดาว + หน้าจอสั่น (ใช้ร่วมกัน) |
| `ClientSetup.java` | ลงทะเบียน renderer และ overlay (client) |

## สถานะการทดสอบ (1.1.0)
ทดสอบ/ตรวจแล้ว
- GeckoLib 4.7.4: ตรวจกับซอร์สจริงของ GeckoLib (branch 1.20.1 ที่ commit "Update to 4.7.4") ชื่อ package/class (`GeoEntity`, `core.animation.*`, `DefaultedEntityGeoModel`, `GeoEntityRenderer`), signature ของ `registerControllers` และ constructor ของ `AnimationController` ตรงกับโค้ด, repo cloudsmith และ coordinates `software.bernie.geckolib:geckolib-forge-1.20.1` ถูกต้อง
- Forge API ที่ใช้ใหม่ (`RegisterGuiOverlaysEvent`, `IGuiOverlay`, `ViewportEvent.ComputeFov`, `MovementInputUpdateEvent`, `putBulkData` แบบมี alpha, access transformer ของ constructor `Projectile`) ตรวจกับซอร์ส Forge 1.20.1 แล้ว
- signature ของ vanilla 1.20.1 ที่เสี่ยง (`Item.onUseTick/getUseDuration/getUseAnimation`, `ItemRenderer.renderStatic/getModel`, `RenderType.entityTranslucentEmissive/lightning`) ตรวจกับ mapping official 1.20.1 แล้ว
- โค้ดทั้งหมด compile ผ่าน javac กับ stub ของ API (ตรวจ type, import, lambda) และตรวจคณิตศาสตร์การหมุนดาบให้ใบดาบชี้ไปทางที่บิน
- texture Zenith ถอดจากรูป pixel art ที่แนบมา (ตารางพิกเซลเอียง 45° → 28×28 สะอาด แล้วขยาย 4 เท่าเป็น 112×112)

ยังไม่ได้ทดสอบ (ต้องรอ build จริง/เข้าเกม)
- ยังไม่ได้ build ด้วย ForgeGradle จริง เพราะเครื่องที่ใช้เขียนเข้า maven ของ Forge/Mojang/GeckoLib ไม่ได้ → ให้ build ผ่าน GitHub Actions แล้วส่ง log มาถ้ามี error
- ยังไม่ได้เห็นในเกม: ขนาด/ทิศของดาบที่บิน, ความหนาแน่นของ particle, impact frame, ความแรงหน้าจอสั่น อาจต้องปรับตัวเลขตามหัวข้อ "ปรับค่าต่างๆ"
- โมเดลดาว (geo.json) ยังไม่ได้เปิดใน Blockbench

## ประวัติการเปลี่ยนแปลง
1.1.0
- เพิ่ม Zenith Sword (`starfury:zenith_sword`) พร้อม entity `starfury:zenith_blade` และ texture ดาบวิญญาณ 4 สี
- `build.gradle`: mclib เปลี่ยนจาก `implementation` เป็น `minecraftLibrary` (ให้ `runClient` ทำงานถูกกับ ForgeGradle 6), ใส่เวอร์ชันจาก `gradle.properties` ลง `mods.toml` อัตโนมัติ
- ดาวตก: ย้ายจุดศูนย์กลางโมเดลให้อยู่กลาง hitbox, ปรับมุม rotation ให้อยู่ในช่วง ±180°, ลดขนาดจาก scale 1.8–2.3 (กว้าง ~3.7 บล็อก) เป็น 1.0–1.3 (กว้าง ~1.6–2.1 บล็อก) ถ้าชอบขนาดเดิมให้แก้ค่า scale ใน `fallen_star.animation.json`
- texture Starfury ขยายจาก 24×24 เป็น 48×48 (หน้าตาเหมือนเดิมทุกพิกเซล) เพื่อไม่ให้ Minecraft ลดระดับ mipmap ของทั้ง atlas
