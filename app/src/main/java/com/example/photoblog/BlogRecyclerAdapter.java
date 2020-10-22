package com.example.photoblog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {
    public List<BlogPost> blog_list;
    public List<User> user_list;
    public Context context;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    public BlogRecyclerAdapter (List<BlogPost> blog_list, List<User> user_list){
        this.blog_list = blog_list;
        this.user_list=user_list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item, parent, false);
        context = parent.getContext();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        String blogPostId = blog_list.get(position).BlogPostId;
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String desc_data = blog_list.get(position).getDesc();
        holder.setDescText(desc_data);

        String blog_user_id = blog_list.get(position).getUser_id();

        if (blog_user_id.equals(currentUserId)){
            holder.blogDeleteBtn.setEnabled(true);
            holder.blogDeleteBtn.setVisibility(View.VISIBLE);
        }

        String image_uri = blog_list.get(position).getImage_url();
        String thumbUri = blog_list.get(position).getImage_thumb();
        firebaseFirestore.collection("Posts").document(blog_user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    //String image_uri = blog_list.get(position).getImage_url();
                    holder.setBlogImage(image_uri, thumbUri);
                } else {
                    Toast.makeText(context, "Error with image", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //holder.setBlogImage(image_uri);

        String userName = user_list.get(position).getName();
        String userImage = user_list.get(position).getImage();
        holder.setUserData(userName, userImage);

        long millisecond = blog_list.get(position).getTimesamp().getTime();
        String dateString = DateFormat.format("MM/dd/yyyy", new Date(millisecond)).toString();
        holder.setTime(dateString);

        firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (!value.isEmpty()){
                    int count = value.size();
                    holder.updateLikesCount(count);
                } else {
                    holder.updateLikesCount(0);
                }
            }
        });

        firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                if (documentSnapshot.exists()){
                    holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_accent));
                } else {
                    holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_gray));
                }
            }
        });

        holder.blogLikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.getResult().exists()){
                            Map<String, Object> likesMap = new HashMap<>();
                            likesMap.put("timesamp", FieldValue.serverTimestamp());
                            firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(currentUserId).set(likesMap);
                        } else {
                            firebaseFirestore.collection("Posts/"+blogPostId+"/Likes").document(currentUserId).delete();
                        }
                    }
                });
            }
        });

        holder.blogCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent commentIntent = new Intent(context, CommentsActivity.class);
                commentIntent.putExtra("blog_post_id", blogPostId);
                context.startActivity(commentIntent);
            }
        });

        holder.blogDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseFirestore.collection("Posts").document(blogPostId).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        blog_list.remove(position);
                        user_list.remove(position);
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private View mView;
        private TextView descView;
        private ImageView blogImageView;
        private TextView blogDate;

        private TextView blogUserName;
        private CircleImageView blogUserImage;

        private ImageView blogLikeBtn;
        private TextView blogLikeCount;
        private ImageView blogCommentBtn;
        private Button blogDeleteBtn;
        private TextView blogCommentCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            blogLikeBtn = mView.findViewById(R.id.blog_like_btn);
            blogCommentBtn = mView.findViewById(R.id.blog_comment_btn);
            blogDeleteBtn = mView.findViewById(R.id.blog_delete_btn);
        }
        public void setDescText(String descText){
            descView = mView.findViewById(R.id.blog_desc);
            descView.setText(descText);
        }
        public void setBlogImage(String downloadUri, String thumbUri){
            blogImageView = mView.findViewById(R.id.blog_image);
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.profile);
            Glide.with(context).applyDefaultRequestOptions(requestOptions).load(downloadUri).thumbnail(Glide.with(context).load(thumbUri)).into(blogImageView);
        }
        public void setTime(String date){
            blogDate = mView.findViewById(R.id.blog_date);
            blogDate.setText(date);
        }
        public void setUserData(String name, String image){
            blogUserImage = mView.findViewById(R.id.blog_user_image);
            blogUserName = mView.findViewById(R.id.blog_user_name);
            blogUserName.setText(name);
            RequestOptions placeholderOption = new RequestOptions();
            placeholderOption.placeholder(R.drawable.profile);
            Glide.with(context).applyDefaultRequestOptions(placeholderOption).load(image).into(blogUserImage);
        }
        public void updateLikesCount(int count){
            blogLikeCount = mView.findViewById(R.id.blog_like_count);
            blogLikeCount.setText(count + "Likes");
        }
    }
}
