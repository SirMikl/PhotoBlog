package com.example.photoblog;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    private RecyclerView blog_list_view;
    private List<BlogPost> blog_list;
    private List<User> user_list;
    private FirebaseFirestore firebaseFirestore;
    private BlogRecyclerAdapter blogRecyclerAdapter;
    private FirebaseAuth firebaseAuth;
    private DocumentSnapshot lastVisible;
    private Boolean idFirstPageFirstLoad = true;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        blog_list = new ArrayList<>();
        user_list = new ArrayList<>();
        blog_list_view = view.findViewById(R.id.blog_list_view);
        firebaseAuth = FirebaseAuth.getInstance();
        blogRecyclerAdapter = new BlogRecyclerAdapter(blog_list, user_list);
        blog_list_view.setLayoutManager(new LinearLayoutManager(container.getContext()));
        blog_list_view.setAdapter(blogRecyclerAdapter);
        if (firebaseAuth.getCurrentUser() != null){
        firebaseFirestore = FirebaseFirestore.getInstance();
        blog_list_view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Boolean reachedBottom = !recyclerView.canScrollVertically(1);
                if (reachedBottom){
                    String desc = lastVisible.getString("desc");
                    Toast.makeText(container.getContext(), "Reached "+desc, Toast.LENGTH_SHORT).show();
                    loadMorePost();
                }
            }
        });
        Query firstQuery = firebaseFirestore.collection("Posts").orderBy("timesamp", Query.Direction.DESCENDING).limit(3);
        firstQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException error) {
                if (!documentSnapshots.isEmpty()){
                if (idFirstPageFirstLoad){
                lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size()-1);
                blog_list.clear();
                user_list.clear();
                }
                for (DocumentChange doc: documentSnapshots.getDocumentChanges()){
                    if (doc.getType() == DocumentChange.Type.ADDED){
                        String blogPostId = doc.getDocument().getId();
                        BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);
                        String blogUserId = doc.getDocument().getString("user_id");
                        firebaseFirestore.collection("Users").document(blogUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()){
                                    User user = task.getResult().toObject(User.class);
                                    if (idFirstPageFirstLoad){
                                        user_list.add(user);
                                        blog_list.add(blogPost);
                                    } else {
                                        user_list.add(0, user);
                                        blog_list.add(0, blogPost);
                                    }
                                    blogRecyclerAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
                idFirstPageFirstLoad = false;
            }
            }
        });
        }
        return view;
    }

    public void loadMorePost(){
        Query nextQuery = firebaseFirestore.collection("Posts").orderBy("timesamp", Query.Direction.DESCENDING).startAfter(lastVisible).limit(3);
        nextQuery.addSnapshotListener(getActivity(), new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException error) {
                if(!documentSnapshots.isEmpty()){
                lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size()-1);
                for (DocumentChange doc: documentSnapshots.getDocumentChanges()){
                    if (doc.getType() == DocumentChange.Type.ADDED){
                        String blogPostId = doc.getDocument().getId();
                        BlogPost blogPost = doc.getDocument().toObject(BlogPost.class).withId(blogPostId);
                        String blogUserId = doc.getDocument().getString("user_id");
                        firebaseFirestore.collection("Users").document(blogUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()){
                                    User user = task.getResult().toObject(User.class);
                                    user_list.add(user);
                                    blog_list.add(blogPost);
                                    blogRecyclerAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }}
                }
            }
        });
    }
}