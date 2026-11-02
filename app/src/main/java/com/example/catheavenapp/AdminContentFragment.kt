package com.example.catheavenapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catheavenapp.databinding.DialogCatEditorBinding
import com.example.catheavenapp.databinding.DialogMenuItemEditorBinding
import com.example.catheavenapp.databinding.FragmentAdminContentBinding
import com.example.catheavenapp.domain.booking.VisitDate
import com.example.catheavenapp.domain.content.ContentCheck
import com.example.catheavenapp.domain.content.ContentField
import com.example.catheavenapp.domain.model.AdoptionStatus
import com.example.catheavenapp.domain.model.Cat
import com.example.catheavenapp.domain.model.CatDescriptions
import com.example.catheavenapp.domain.model.MenuCategory
import com.example.catheavenapp.domain.model.MenuItem
import com.example.catheavenapp.ui.admin.AdminContentViewModel
import com.example.catheavenapp.ui.admin.CatPhotos
import com.example.catheavenapp.ui.admin.ContentListState
import com.example.catheavenapp.ui.admin.ContentRow
import com.example.catheavenapp.ui.admin.ContentSaveState
import com.example.catheavenapp.ui.admin.ContentTab
import com.example.catheavenapp.ui.booking.formatDateMillis
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.TimeZone

/** Admin screen for managing the resident cats and the café / gift shop menu. */
class AdminContentFragment : Fragment() {

    private var _binding: FragmentAdminContentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminContentViewModel by viewModels()
    private lateinit var adapter: AdminContentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminContentAdapter(onEdit = ::openEditor, onDelete = ::confirmDelete)
        binding.adminContentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.adminContentRecyclerView.adapter = adapter

        // Toggle between the cats list and the menu list
        binding.contentToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnManageCats -> viewModel.selectTab(ContentTab.CATS)
                R.id.btnManageMenu -> viewModel.selectTab(ContentTab.MENU)
            }
        }

        viewModel.tab.observe(viewLifecycleOwner) { tab ->
            val buttonId = if (tab == ContentTab.MENU) R.id.btnManageMenu else R.id.btnManageCats
            if (binding.contentToggleGroup.checkedButtonId != buttonId) binding.contentToggleGroup.check(buttonId)
            binding.fabAddContent.contentDescription =
                if (tab == ContentTab.MENU) "Add a menu item" else "Add a new cat"
        }

        viewModel.list.observe(viewLifecycleOwner) { render(it) }

        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            binding.fabAddContent.isEnabled = state != ContentSaveState.Saving
            when (state) {
                is ContentSaveState.Done -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                    viewModel.onSaveMessageShown()
                }
                is ContentSaveState.Failed -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.onSaveMessageShown()
                }
                ContentSaveState.Idle, ContentSaveState.Saving -> Unit
            }
        }

        binding.fabAddContent.setOnClickListener {
            when (viewModel.tab.value) {
                ContentTab.MENU -> showMenuEditor(null)
                else -> showCatEditor(null)
            }
        }

        if (viewModel.list.value !is ContentListState.Loaded) viewModel.load()
    }

    private fun render(state: ContentListState) {
        when (state) {
            ContentListState.Loading -> showMessage("Loading…")
            is ContentListState.Failed -> showMessage(state.message)
            is ContentListState.Loaded -> {
                adapter.updateData(state.rows)
                if (state.rows.isEmpty()) {
                    showMessage(
                        if (state.tab == ContentTab.CATS) "No cats yet. Tap + to add one."
                        else "No menu items yet. Tap + to add one."
                    )
                } else {
                    binding.emptyStateText.visibility = View.GONE
                }
            }
        }
    }

    private fun showMessage(text: String) {
        adapter.updateData(emptyList())
        binding.emptyStateText.text = text
        binding.emptyStateText.visibility = View.VISIBLE
    }

    private fun openEditor(row: ContentRow) {
        when (row) {
            is ContentRow.CatRow -> showCatEditor(row.cat)
            is ContentRow.MenuRow -> showMenuEditor(row.item)
        }
    }

    private fun confirmDelete(row: ContentRow) {
        val (name, message) = when (row) {
            is ContentRow.CatRow -> row.cat.name to "${row.cat.name} will be removed from the residents list for everyone."
            is ContentRow.MenuRow -> row.item.name to "${row.item.name} will be removed from the menu."
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete $name?")
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                when (row) {
                    is ContentRow.CatRow -> viewModel.deleteCat(row.cat)
                    is ContentRow.MenuRow -> viewModel.deleteMenuItem(row.item)
                }
            }
            .show()
    }

    // ---- Cat editor ----

    private fun showCatEditor(existing: Cat?) {
        val form = DialogCatEditorBinding.inflate(layoutInflater)
        val context = requireContext()

        var dateOfBirth = existing?.dateOfBirthMillis
        var dateArrived = existing?.dateArrivedMillis
        var status = existing?.adoptionStatus ?: AdoptionStatus.RESIDENT
        var photo = existing?.photoUrl ?: CatPhotos.NONE
        var gender = existing?.gender?.lowercase().orEmpty()

        form.catNameEdit.setText(existing?.name.orEmpty())
        form.catBreedEdit.setText(existing?.breed.orEmpty())
        form.catPersonalityEdit.setText(existing?.personality.orEmpty())
        form.catBiographyEdit.setText(existing?.biography.orEmpty())

        val genders = listOf("female", "male", "")
        form.catGenderDropdown.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_list_item_1, genders.map(::genderLabel))
        )
        form.catGenderDropdown.setText(genderLabel(gender), false)
        form.catGenderDropdown.setOnItemClickListener { _, _, position, _ -> gender = genders[position] }

        val statuses = AdoptionStatus.entries
        form.catStatusDropdown.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_list_item_1, statuses.map(CatDescriptions::statusLabel))
        )
        form.catStatusDropdown.setText(CatDescriptions.statusLabel(status), false)
        form.catStatusDropdown.setOnItemClickListener { _, _, position, _ -> status = statuses[position] }

        form.catPhotoDropdown.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_list_item_1, CatPhotos.KEYS.map(CatPhotos::label))
        )
        form.catPhotoDropdown.setText(CatPhotos.label(photo), false)
        form.catPhotoDropdown.setOnItemClickListener { _, _, position, _ -> photo = CatPhotos.KEYS[position] }

        fun showDates() {
            form.btnDateOfBirth.text = "Date of birth: ${dateOfBirth?.let(::formatDateMillis) ?: "not set"}"
            form.btnDateArrived.text = "Arrived: ${dateArrived?.let(::formatDateMillis) ?: "not set"}"
            form.btnClearDateOfBirth.isEnabled = dateOfBirth != null
            form.btnClearDateArrived.isEnabled = dateArrived != null
        }
        showDates()
        form.btnDateOfBirth.setOnClickListener {
            pickDate("Date of birth", dateOfBirth) { dateOfBirth = it; showDates() }
        }
        form.btnDateArrived.setOnClickListener {
            pickDate("Date arrived at Cat Heaven", dateArrived) { dateArrived = it; showDates() }
        }
        form.btnClearDateOfBirth.setOnClickListener { dateOfBirth = null; showDates() }
        form.btnClearDateArrived.setOnClickListener { dateArrived = null; showDates() }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) "Add a cat" else "Edit ${existing.name}")
            .setView(form.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)   // set below, so an invalid form keeps the dialog open
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                clearCatErrors(form)
                val cat = Cat(
                    id = existing?.id.orEmpty(),
                    name = form.catNameEdit.text?.toString()?.trim().orEmpty(),
                    dateOfBirthMillis = dateOfBirth,
                    gender = gender,
                    breed = form.catBreedEdit.text?.toString()?.trim().orEmpty(),
                    dateArrivedMillis = dateArrived,
                    personality = form.catPersonalityEdit.text?.toString()?.trim().orEmpty(),
                    biography = form.catBiographyEdit.text?.toString()?.trim().orEmpty(),
                    adoptionStatus = status,
                    photoUrl = photo
                )
                when (val check = viewModel.saveCat(cat)) {
                    ContentCheck.Valid -> dialog.dismiss()
                    is ContentCheck.Invalid -> showCatError(form, check)
                }
            }
        }
        dialog.show()
    }

    private fun genderLabel(value: String): String =
        if (value.isBlank()) "Unknown" else value.replaceFirstChar { it.uppercase() }

    private fun clearCatErrors(form: DialogCatEditorBinding) {
        listOf(
            form.catNameLayout, form.catBreedLayout, form.catGenderLayout, form.catStatusLayout,
            form.catPhotoLayout, form.catPersonalityLayout, form.catBiographyLayout
        ).forEach { it.error = null }
        form.catDateError.visibility = View.GONE
    }

    private fun showCatError(form: DialogCatEditorBinding, check: ContentCheck.Invalid) {
        val layout = when (check.field) {
            ContentField.NAME -> form.catNameLayout
            ContentField.BREED -> form.catBreedLayout
            ContentField.GENDER -> form.catGenderLayout
            ContentField.PERSONALITY -> form.catPersonalityLayout
            ContentField.BIOGRAPHY -> form.catBiographyLayout
            ContentField.PHOTO -> form.catPhotoLayout
            ContentField.DATE_OF_BIRTH, ContentField.DATE_ARRIVED, ContentField.PRICE -> null
        }
        if (layout != null) {
            layout.error = check.message
            layout.requestFocus()
        } else {
            form.catDateError.text = check.message
            form.catDateError.visibility = View.VISIBLE
        }
    }

    /**
     * Opens a calendar limited to today and earlier. The picker works in UTC, so the chosen
     * day is converted to local midnight to match how the rest of the app stores dates.
     */
    private fun pickDate(title: String, currentLocalMillis: Long?, onPicked: (Long) -> Unit) {
        val utc = TimeZone.getTimeZone("UTC")
        val selection = currentLocalMillis
            ?.let { VisitDate.fromEpochMillis(it).toEpochMillis(utc) }
            ?: MaterialDatePicker.todayInUtcMilliseconds()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(selection)
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
            )
            .build()
        picker.addOnPositiveButtonClickListener { picked ->
            onPicked(VisitDate.fromEpochMillis(picked, utc).toEpochMillis())
        }
        picker.show(childFragmentManager, "catDatePicker")
    }

    // ---- Menu item editor ----

    private fun showMenuEditor(existing: MenuItem?) {
        val form = DialogMenuItemEditorBinding.inflate(layoutInflater)
        val context = requireContext()

        var category = existing?.category ?: MenuCategory.FOOD_AND_DRINK
        form.menuNameEdit.setText(existing?.name.orEmpty())
        form.menuPriceEdit.setText(existing?.price?.toString().orEmpty())
        form.menuAvailableSwitch.isChecked = existing?.isAvailable ?: true

        val categories = MenuCategory.entries
        form.menuCategoryDropdown.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_list_item_1, categories.map { it.label })
        )
        form.menuCategoryDropdown.setText(category.label, false)
        form.menuCategoryDropdown.setOnItemClickListener { _, _, position, _ -> category = categories[position] }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) "Add a menu item" else "Edit ${existing.name}")
            .setView(form.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                form.menuNameLayout.error = null
                form.menuPriceLayout.error = null

                val priceText = form.menuPriceEdit.text?.toString()?.trim().orEmpty()
                val price = priceText.toIntOrNull()
                if (priceText.isNotEmpty() && price == null) {
                    form.menuPriceLayout.error = "Enter the price in whole rand, e.g. 35."
                    return@setOnClickListener
                }
                val item = MenuItem(
                    id = existing?.id.orEmpty(),
                    name = form.menuNameEdit.text?.toString()?.trim().orEmpty(),
                    category = category,
                    price = price,
                    isAvailable = form.menuAvailableSwitch.isChecked
                )
                when (val check = viewModel.saveMenuItem(item)) {
                    ContentCheck.Valid -> dialog.dismiss()
                    is ContentCheck.Invalid -> {
                        val layout = if (check.field == ContentField.PRICE) form.menuPriceLayout else form.menuNameLayout
                        layout.error = check.message
                        layout.requestFocus()
                    }
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
