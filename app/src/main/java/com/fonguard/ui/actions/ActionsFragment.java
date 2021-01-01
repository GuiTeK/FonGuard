/*
 * FonGuard
 * Copyright (C) 2021  Guillaume TRUCHOT <guillaume.truchot@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fonguard.ui.actions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.fonguard.R;

public class ActionsFragment extends Fragment {
    //private RecyclerView mActionsRecyclerView;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_actions, container, false);
        /*final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        final HttpActionAdapter adapter = new HttpActionAdapter();

        mActionsRecyclerView = root.findViewById(R.id.actions_recyclerView);
        mActionsRecyclerView.setLayoutManager(linearLayoutManager);
        // C.F. https://stackoverflow.com/questions/28709220/understanding-recyclerview-sethasfixedsize
        mActionsRecyclerView.setHasFixedSize(true);
        mActionsRecyclerView.setAdapter(adapter);*/

        return root;
    }
}
