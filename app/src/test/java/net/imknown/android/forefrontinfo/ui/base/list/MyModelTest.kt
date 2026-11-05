package net.imknown.android.forefrontinfo.ui.base.list

import net.imknown.android.forefrontinfo.ui.theme.StatusColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// The row identity contract both the list UI and the golden snapshot compare on.
class MyModelTest {

    @Test
    fun `resource title keys by resource id`() {
        val model = MyModel(title = MyModelTitle.Res(42), detail = "detail")

        assertEquals("42", model.key)
    }

    @Test
    fun `raw title keys by its own text`() {
        val model = MyModel(title = MyModelTitle.Raw("ro.build.type"), detail = "detail")

        assertEquals("ro.build.type", model.key)
    }

    // The values every row that sets neither field carries; the snapshot records them per row.
    @Test
    fun `defaults are no color and the general type`() {
        val model = MyModel(title = MyModelTitle.Raw("ro.build.type"), detail = "detail")

        assertEquals(StatusColor.NONE, model.color)
        assertEquals(MyModelType.General, model.type)
    }
}
